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

package models

import controllers.auth.AuthenticatedRequest
import play.api.libs.json.Json
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

case class Ticket private (
  name: String,
  email: String,
  subject: String,
  message: String,
  referrer: String,
  javascriptEnabled: String,
  userAgent: String,
  authId: String,
  areaOfTax: String,
  sessionId: String,
  userTaxIdentifiers: UserTaxIdentifiers)

object UserTaxIdentifiers {
  implicit val formats = Json.format[UserTaxIdentifiers]
}

object Ticket extends FieldTransformer {

  implicit val formats = Json.format[Ticket]

  def create(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, hc: HeaderCarrier, request: AuthenticatedRequest[_]): Ticket =
    Ticket(
      name.trim,
      email,
      subject,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(request, hc),
      areaOfTaxOf(request),
      sessionIdFrom(hc),
      UserTaxIdentifiers(request.nino, request.ctUtr, request.saUtr, request.vrn, request.payeEmpRef))
}

object TicketId {
  implicit val formats = Json.format[TicketId]
}

case class TicketId(ticket_id: Int)

case class UserTaxIdentifiers(
  nino: Option[Nino],
  ctUtr: Option[CtUtr],
  utr: Option[SaUtr],
  vrn: Option[Vrn],
  empRef: Option[EmpRef])

case class Feedback(
  name: String,
  email: String,
  subject: String,
  rating: String,
  message: String,
  referrer: String,
  javascriptEnabled: String,
  userAgent: String,
  authId: String,
  areaOfTax: String,
  sessionId: String,
  userTaxIdentifiers: UserTaxIdentifiers)

object Feedback extends FieldTransformer {

  implicit val formats = Json.format[Feedback]

  def create(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, hc: HeaderCarrier, request: AuthenticatedRequest[_]): Feedback =
    Feedback(
      name.trim,
      email,
      subject,
      rating,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(request, hc),
      areaOfTaxOf(request),
      sessionIdFrom(hc),
      UserTaxIdentifiers(request.nino, request.ctUtr, request.saUtr, request.vrn, request.payeEmpRef))
}

trait FieldTransformer {
  val NA = "n/a"
  val taxCreditRenewals = "taxcreditrenewals"

  def sessionIdFrom(hc: HeaderCarrier) = hc.sessionId.map(_.value).getOrElse("n/a")

  def areaOfTaxOf(request:AuthenticatedRequest[_]) = request.session.get(SessionKeys.authProvider) match {
    // TODO IDA in the future will not only be for PAYE so another way will be required to map to area of tax
    case Some("IDA") => "paye"
    case Some("GGW") => "biztax"
    case Some("NTC") => taxCreditRenewals
    case _           => NA
  }

  def userIdFrom(request: AuthenticatedRequest[_], hc: HeaderCarrier): String = request.session.get(SessionKeys.sensitiveUserId) match {
    case Some("true") => NA
    case _ => hc.userId.map(_.value).getOrElse(NA)
  }

  def userAgentOf(request: AuthenticatedRequest[_]) = request.headers.get("User-Agent").getOrElse("n/a")

  def ynValueOf(javascript: Boolean) = if (javascript) "Y" else "N"

}
