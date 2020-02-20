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

package controllers.paye

 import config.{AppFormPartialRetriever, ApplicationConfig}
 import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
 import models.PayeAtsData
 import play.api.Play
 import play.api.mvc.Results.Redirect
 import play.api.mvc.{Action, AnyContent}
 import services.PayeAtsService
 import uk.gov.hmrc.http.HttpResponse
 import uk.gov.hmrc.play.frontend.controller.FrontendController
 import play.api.Play.current
 import play.api.i18n.Messages.Implicits._

 import scala.concurrent.Future

object PayeGovernmentSpendController extends PayeGovernmentSpendController{

  override val payeAuthAction = Play.current.injector.instanceOf[PayeAuthAction]
  override val payeAtsService = PayeAtsService
  override val payeYear: Int = ApplicationConfig.payeYear
}

trait PayeGovernmentSpendController extends FrontendController {

  implicit val formPartialRetriever = AppFormPartialRetriever

  val payeAuthAction: PayeAuthAction
  val payeAtsService: PayeAtsService
  val payeYear: Int

  def show: Action[AnyContent] = payeAuthAction.async {
    implicit request: PayeAuthenticatedRequest[_] =>

      payeAtsService.getPayeATSData(request.nino, payeYear).map {
        case Right(successResponse: PayeAtsData) => Ok("")
        case Left(response: HttpResponse) =>
          response.status match {
          case 404 => Redirect(controllers.routes.ErrorController.authorisedNoAts())
          case _ => BadRequest("Bad request")
        }
      }
  }
}
