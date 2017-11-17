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

package services

import connectors.AuthenticationConnector
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.model.DataEvent
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.{ HeaderCarrier, HeaderNames }


object AuditTypes {
  val Tx_FAILED = "TxFailed"
  val Tx_SUCCEEDED = "TxSuccessful"
}

object AuditService extends AuditService

trait AuditService extends AuthenticationConnector {

  val taxsAuditSource = "tax-summaries-frontend"

  def sendEvent(auditType: String, details: Map[String, String], sessionId: Option[String] = None)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(eventFor(auditType, details, sessionId))

  def eventFor(auditType: String, details: Map[String, String], sessionId: Option[String])(implicit hc: HeaderCarrier) =
    DataEvent(
      auditSource = taxsAuditSource,
      auditType = auditType,
      tags = Map(HeaderNames.xSessionId -> sessionId.getOrElse("")) ++ hc.headers.toMap,
      detail = details)
}
