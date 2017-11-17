/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors.deskpro

import config.WSHttp
import connectors.deskpro.domain.{Feedback, TicketId, Ticket}
import play.api.mvc.Request
import uk.gov.hmrc.play.config.ServicesConfig
import scala.concurrent.Future
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.http.{ HeaderCarrier, HttpPost }

object HmrcDeskproConnector extends HmrcDeskproConnector with ServicesConfig {
  override lazy val serviceUrl = baseUrl("hmrc-deskpro")
  override val http = WSHttp
}

trait HmrcDeskproConnector {


  def serviceUrl: String

  def http: HttpPost

  def createTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], userOption: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {

    createDeskProTicket(name, email, subject, message, referrer, isJavascript, request, userOption.map(_.principal.accounts))
  }

  def createDeskProTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], accountsOption: Option[Accounts])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
    http.POST[Ticket, TicketId](requestUrl("/deskpro/ticket"), Ticket.create(name, email, subject, message, referrer, isJavascript, hc, request, accountsOption)).map(Some(_))
  }

  def createFeedback(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], userOption: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
    http.POST[Feedback, TicketId](requestUrl("/deskpro/feedback"), Feedback.create(name, email, rating, subject, message, referrer, isJavascript, hc, request, userOption)).map(Some(_))
  }

  private def requestUrl[B, A](uri: String): String = s"$serviceUrl$uri"
}
