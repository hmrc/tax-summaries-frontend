/*
 * Copyright 2026 HM Revenue & Customs
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

package testOnly.controllers

import com.google.inject.Inject
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

class GenerateAgentTokenController @Inject() (mcc: MessagesControllerComponents)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = Action { implicit request =>

    import uk.gov.hmrc.crypto.{AesCrypto, PlainText}

      import java.net.URLEncoder
      import java.time.Instant

    val encKey = "1111111111111111111111"

    val crypto = new AesCrypto {
      override protected val encryptionKey: String = encKey
    }

    val agentId = "V3264H" // <- Put agent id here
    val utr     = "1130492359" // <- Put UTR here

    val token =
      URLEncoder.encode(crypto.encrypt(PlainText(s"$agentId:$utr:" + (Instant.now.toEpochMilli))).value, "UTF-8")

    Ok(Html(token))

  }

}
