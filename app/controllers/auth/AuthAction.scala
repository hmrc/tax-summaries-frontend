/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import config.{ApplicationConfig, WSHttp}
import play.api.Mode.Mode
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.{Configuration, Play}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Nino => AuthNino, ConfidenceLevel, Enrolment, Enrolments, InsufficientConfidenceLevel, InsufficientEnrolments, NoActiveSession, PlayAuthConnector}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{CtUtr, EmpRef, SaUtr, Uar, Vrn, Nino}
import uk.gov.hmrc.http.{CorePost, HeaderCarrier}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(override val authConnector: AuthConnector,
                               configuration: Configuration)(implicit ec: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L50 and (Enrolment("IR-SA") or Enrolment("IR-SA-AGENT")) or AuthNino(hasNino = true))
      .retrieve(Retrievals.allEnrolments and Retrievals.externalId and Retrievals.nino) {
        case Enrolments(enrolments) ~ Some(externalId) ~ nino => {
          val agentRef: Option[Uar] = enrolments.find(_.key == "IR-SA-AGENT").flatMap { enrolment =>
            enrolment.identifiers
              .find(id => id.key == "IRAgentReference")
              .map(key => Uar(key.value))
          }
          val saUtr: Option[SaUtr] = enrolments.find(_.key == "IR-SA").flatMap { enrolment =>
            enrolment.identifiers
              .find(id => id.key == "UTR")
              .map(key => SaUtr(key.value))
          }

          val payeEmpRef: Option[EmpRef] = enrolments.find(_.key == "IR-PAYE")
            .map { enrolment =>
              val taxOfficeNumber = enrolment.identifiers.find(id => id.key == "TaxOfficeNumber").map(_.value)
              val taxOfficeReference = enrolment.identifiers.find(id => id.key == "TaxOfficeReference").map(_.value)

              (taxOfficeNumber, taxOfficeReference) match {
                case (Some(number), Some(reference)) =>
                  EmpRef(number, reference)
              }
            }

          val ctUtr: Option[CtUtr] = enrolments.find(_.key == "IR-CT").flatMap { enrolment =>
            enrolment.identifiers
              .find(id => id.key == "UTR")
              .map(key => CtUtr(key.value))
          }

          val vrn: Option[Vrn] = enrolments.find(vatEnrolments => vatEnrolments.key == "HMCE-VATDEC-ORG" || vatEnrolments.key == "HMCE-VATVAR-ORG")
            .flatMap { enrolment =>
              enrolment.identifiers
                .find(id => id.key == "VATRegNo")
                .map(key => Vrn(key.value))
            }

          block {
            AuthenticatedRequest(
              externalId,
              agentRef,
              saUtr,
              nino.map(Nino),
              payeEmpRef,
              ctUtr,
              vrn,
              request
            )
          }
        }
        case _ => throw new RuntimeException("Can't find credentials for user")
      }
  } recover {
    case _: NoActiveSession => {
      lazy val ggSignIn = ApplicationConfig.loginUrl
      lazy val callbackUrl = ApplicationConfig.loginCallback
      Redirect(
        ggSignIn,
        Map(
          "continue"    -> Seq(callbackUrl),
          "origin"      -> Seq(ApplicationConfig.appName)
        )
      )
    }

    case _: InsufficientEnrolments => Redirect(controllers.routes.ErrorController.notAuthorised())
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]

class AuthConnector extends PlayAuthConnector with ServicesConfig {
  override lazy val serviceUrl: String = baseUrl("auth")

  override def http: CorePost = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
