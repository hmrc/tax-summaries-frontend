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

package connectors

import config.WSHttp
import models.AtsData
import models.{AtsData, AtsListData}
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}

object MiddleConnector extends MiddleConnector with ServicesConfig {

  override val serviceUrl = baseUrl("tax-summaries")
  override val agentServiceUrl = baseUrl("tax-summaries-agent")

  override def http = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait MiddleConnector {

  def http: HttpGet

  def serviceUrl: String
  def agentServiceUrl: String = ""

  def url(path: String) = s"$serviceUrl$path"
  def agentUrl(path: String) = s"$agentServiceUrl$path"

  def connectToAts(UTR: SaUtr, taxYear: Int)(implicit hc: HeaderCarrier): Future[AtsData] =
    http.GET[AtsData](url("/taxs/" + UTR + "/" + taxYear + "/ats-data"))

  def connectToAtsOnBehalfOf(uar: Uar, requestedUTR: SaUtr, taxYear: Int)(implicit hc: HeaderCarrier): Future[AtsData] = {
    http.GET[AtsData](agentUrl("/taxsagent/" + uar + "/" + taxYear + "/ats-data/obo/" + requestedUTR))}

  def connectToAtsList(UTR: SaUtr)(implicit hc: HeaderCarrier): Future[AtsListData] = {
    http.GET[AtsListData](url("/taxs/" + UTR + "/ats-list"))}

  def connectToAtsListOnBehalfOf(uar: Uar, requestedUTR: SaUtr)(implicit hc: HeaderCarrier): Future[AtsListData] = {
    http.GET[AtsListData](agentUrl("/taxsagent/" + uar + "/ats-list/obo/" + requestedUTR))}
}
