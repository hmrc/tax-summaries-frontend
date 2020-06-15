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

package controllers

import com.google.inject.Inject
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever

class TaxsLanguageController @Inject()(mcc : MessagesControllerComponents)(implicit val formPartialRetriever: FormPartialRetriever) extends FrontendController(mcc) {

  def switchLanguage(lang: String): Action[AnyContent] = Action { implicit request =>
    request.headers.get(REFERER) match {
        //TODO look for the logic to swtich the language
      case Some(referrer) => Redirect(referrer)
      case _              => Redirect(routes.IndexController.authorisedIndex())
    }
  }
}
