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

import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._
import utils.AuthorityUtils
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.TestConstants._

import scala.concurrent.Future

class ErrorControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val request = FakeRequest()

  "Calling ErrorController with no session" should {

    "return a 303 response" in new ErrorController {

      val result = notAuthorised(request)
      status(result) shouldBe 303
    }
  }

  "Calling ErrorController authorised noATS" should {

    "return a 303 response" in new ErrorController {

      val result = Future.successful(authorisedNoAts(request))
      status(result) shouldBe 303
    }
  }


  "ErrorController" should {

    "Show No ATS page" in new ErrorController {

      val result = noAts(user, request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.title shouldBe "No ATS"

      // Make sure that breadcrumbs are correct
      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3)").toString should include("<strong>No ATS available</strong>")
    }
  }
}
