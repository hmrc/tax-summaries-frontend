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

import com.google.inject.ImplementedBy
import config.ApplicationConfig
import javax.inject.Inject
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.{ExecutionContext, Future}

class RoutingActionImpl @Inject()(override val authConnector: DefaultAuthConnector, mcc: MessagesControllerComponents)(
  implicit appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends RoutingAction with AuthorisedFunctions {

  override def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised().retrieve(Retrievals.allEnrolments and Retrievals.nino) {
      case enrolments ~ optNino =>
        (hasEnrolment(enrolments, "IR-SA") || hasEnrolment(enrolments, "IR-SA-AGENT"), optNino) match {

          case (true, _) => Future.successful(None)
          case (false, Some(_)) =>
            Future.successful(Some(Redirect(controllers.paye.routes.PayeMultipleYearsController.onPageLoad())))
          case _ => Future.successful(Some(Redirect(controllers.routes.ErrorController.authorisedNoAts())))
        }
      case _ => throw new RuntimeException("Can not find enrolments")
    }
  } recover {
    case _: NoActiveSession =>
      lazy val ggSignIn = appConfig.loginUrl
      lazy val callbackUrl = appConfig.loginCallback
      Some(
        Redirect(
          ggSignIn,
          Map(
            "continue" -> Seq(callbackUrl),
            "origin"   -> Seq(appConfig.appName)
          )
        ))

    case _: InsufficientEnrolments => Some(Redirect(controllers.routes.ErrorController.notAuthorised()))
  }

  private def hasEnrolment(enrolments: Enrolments, key: String): Boolean = enrolments.getEnrolment(key).isDefined
  override protected def executionContext: ExecutionContext = ec

  override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser
}

@ImplementedBy(classOf[RoutingActionImpl])
trait RoutingAction extends ActionBuilder[Request, AnyContent] with ActionFilter[Request]