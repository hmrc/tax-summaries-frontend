/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import config.AppFormPartialRetriever
import play.api.Play
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

object TaxsLanguageController extends TaxsLanguageController {
  override def messagesApi: MessagesApi = Play.current.injector.instanceOf(classOf[MessagesApi])
  override val formPartialRetriever = AppFormPartialRetriever
}

trait TaxsLanguageController extends FrontendController with I18nSupport {

  implicit val formPartialRetriever: FormPartialRetriever

  def switchLanguage(lang: String) = Action { implicit request =>
    request.headers.get(REFERER) match {
      case Some(referrer) => Redirect(referrer).withLang(Lang(lang))
      case _ => Redirect(routes.IndexController.authorisedIndex()).withLang(Lang(lang))
    }
  }
}
