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

package connectors.deskpro

import com.google.inject.Inject
import controllers.auth.AuthenticatedRequest
import models.{Feedback, Ticket, TicketId}
import play.api.Configuration
import play.api.Mode.Mode
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

//object HmrcDeskproConnector extends HmrcDeskproConnector with ServicesConfig {
//  override lazy val serviceUrl = baseUrl("hmrc-deskpro")
//  override val http = WSHttp
//
//  override protected def mode: Mode = Play.current.mode
//
//  override protected def runModeConfiguration: Configuration = Play.current.configuration
//}

class HmrcDeskproConnector @Inject()(http: HttpPost, override val runModeConfiguration: Configuration, override val mode: Mode) extends ServicesConfig {

  lazy val serviceUrl: String = baseUrl("hmrc-deskpro")

  def createTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: AuthenticatedRequest[_])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {

    createDeskProTicket(name, email, subject, message, referrer, isJavascript, request)
  }

  def createDeskProTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: AuthenticatedRequest[_])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
    http.POST[Ticket, TicketId](requestUrl("/deskpro/ticket"), Ticket.create(name, email, subject, message, referrer, isJavascript, hc, request)).map(Some(_))
  }

  def createFeedback(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: AuthenticatedRequest[_])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
    http.POST[Feedback, TicketId](requestUrl("/deskpro/feedback"), Feedback.create(name, email, rating, subject, message, referrer, isJavascript, hc, request)).map(Some(_))
  }

  private def requestUrl[B, A](uri: String): String = s"$serviceUrl$uri"
}
