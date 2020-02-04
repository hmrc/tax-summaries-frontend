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
import controllers.auth.paye.{PayeAuthAction, PayeFakeAuthAction}
import controllers.auth.AuthenticatedRequest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.redirectLocation
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._

class PayeIndexControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures with DefaultAwaitTimeout {

  val request = AuthenticatedRequest("userId", None, None, Some(Nino(testNino)), None, None, None, FakeRequest())

  trait TestController extends PayeIndexController {

    override lazy val dataCache = mock[DataCacheConnector]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
    override val authAction: PayeAuthAction = PayeFakeAuthAction
  }

  lazy val  payeTaxYear = ApplicationConfig.payeTaxYear

  "Calling with no params" should {
    "redirect to main page with current paye tax year" in new TestController {

      val result = show(request)

      whenReady(result) { result =>
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(s"/paye/annual-tax-summary/main?taxYear=$payeTaxYear")
      }
    }
  }
}



