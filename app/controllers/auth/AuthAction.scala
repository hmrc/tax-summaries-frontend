/*
 * Copyright 2021 HM Revenue & Customs
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
import config.ApplicationConfig
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.Results.Unauthorized

class AuthActionImpl @Inject()(override val authConnector: DefaultAuthConnector, cc: MessagesControllerComponents)(
  implicit ec: ExecutionContext,
  appConfig: ApplicationConfig)
    extends AuthAction with AuthorisedFunctions {

  override val parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  val saShuttered: Boolean = appConfig.saShuttered

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    if (saShuttered) {
      Future.successful(Redirect(controllers.routes.ErrorController.serviceUnavailable()))
    } else {
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

      authorised(ConfidenceLevel.L50)
        .retrieve(Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr) {
          case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr => {

            val agentRef: Option[Uar] = enrolments.find(_.key == "IR-SA-AGENT").flatMap { enrolment =>
              enrolment.identifiers
                .find(id => id.key == "IRAgentReference")
                .map(key => Uar(key.value))
            }

            val isAgentActive: Boolean = enrolments.find(_.key == "IR-SA-AGENT").map(_.isActivated).getOrElse(false)

            val payeEmpRef: Option[EmpRef] = enrolments
              .find(_.key == "IR-PAYE")
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

            val vrn: Option[Vrn] = enrolments
              .find(vatEnrolments => vatEnrolments.key == "HMCE-VATDEC-ORG" || vatEnrolments.key == "HMCE-VATVAR-ORG")
              .flatMap { enrolment =>
                enrolment.identifiers
                  .find(id => id.key == "VATRegNo")
                  .map(key => Vrn(key.value))
              }

            block {
              AuthenticatedRequest(
                externalId,
                agentRef,
                saUtr.map(SaUtr(_)),
                None,
                payeEmpRef,
                ctUtr,
                vrn,
                saUtr.isDefined,
                isAgentActive,
                credentials,
                request
              )
            }
          }
          case _ => throw new RuntimeException("Can't find credentials for user")
        }
    } recover {
      case _: NoActiveSession => {
        lazy val ggSignIn = appConfig.loginUrl
        lazy val callbackUrl = appConfig.loginCallback
        Redirect(
          ggSignIn,
          Map(
            "continue_url" -> Seq(callbackUrl),
            "origin"       -> Seq(appConfig.appName)
          )
        )
      }

      case _: InsufficientEnrolments => Redirect(controllers.routes.ErrorController.notAuthorised())
    }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]
