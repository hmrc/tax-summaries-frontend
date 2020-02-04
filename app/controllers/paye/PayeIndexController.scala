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
import connectors.DataCacheConnector
import controllers.auth.{AuthAction, AuthenticatedRequest}
import play.api.Play
import play.api.mvc.Result
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever

import scala.concurrent.Future

object PayeIndexController extends PayeIndexController {
  override val formPartialRetriever = AppFormPartialRetriever
  override val authAction = Play.current.injector.instanceOf[AuthAction]
  override lazy val dataCache = DataCacheConnector
}

trait PayeIndexController extends FrontendController {

  implicit val formPartialRetriever: FormPartialRetriever

  val authAction: AuthAction
  val dataCache: DataCacheConnector

  def authorisedIndex = authAction.async {
    request => show(request)
  }

  def show(implicit request: AuthenticatedRequest[_]): Future[Result] = {
    Future(Redirect(routes.PayeAtsMainController.authorisedAtsMain().url + "?taxYear=" + ApplicationConfig.payeTaxYear) )
  }
}