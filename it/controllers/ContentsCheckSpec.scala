/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, post, urlEqualTo, urlMatching, urlPathMatching}
import models.admin.{PertaxBackendToggle, SCAWrapperToggle}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import play.api
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.sca.models.{MenuItemConfig, PtaMinMenuConfig, WrapperDataResponse}
import utils.{FileHelper, IntegrationSpec}

import java.util.UUID
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Random

class ContentsCheckSpec extends IntegrationSpec {

  case class ExpectedData(title: String)

  def getExpectedData(key: String): ExpectedData =
    key match {
      case "main"              =>
        ExpectedData("Service unavailable - Personal tax account - GOV.UK")
      case "summary"           =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "nics"              =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "treasury-spending" =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "income-before-tax" =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "tax-free-amount"   =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "total-income-tax"  =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "capital-gains-tax" =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")

      case "not-authorised"           =>
        ExpectedData("We could not confirm your identity - Annual Tax Summary - GOV.UK")
      case "no-ats"                   =>
        ExpectedData("Bad request - 400 - Annual Tax Summary - GOV.UK")
      case "service-unavailable"      =>
        ExpectedData("Sorry there is a problem with the service - Annual Tax Summary - GOV.UK")
      case "sign-out"                 =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "paye-year"                =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "paye-main"                =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "paye-main-year"           =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "upliftConfidenceLevel"    =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "paye-summary-year"        =>
        ExpectedData("Your income and taxes: 2021 to 2022 - Annual Tax Summary - GOV.UK")
      case "income-insurance-year"    =>
        ExpectedData("Income Tax and National Insurance contributions: 2021 to 2022 - Annual Tax Summary - GOV.UK")
      case "income-before-tax-year"   =>
        ExpectedData("Taxable income: 2021 to 2022 - Annual Tax Summary - GOV.UK")
      case "tax-free-amount-year"     =>
        ExpectedData("Tax-free amount: 2021 to 2022 - Annual Tax Summary - GOV.UK")
      case "not-authorised"           =>
        ExpectedData("We could not confirm your identity - Annual Tax Summary - GOV.UK")
      case "paye-service-unavailable" =>
        ExpectedData("Sorry there is a problem with the service - Annual Tax Summary - GOV.UK")
      case "keepalive"                =>
        ExpectedData("Sign in - Personal tax account - GOV.UK")
      case "session-expired"          =>
        ExpectedData("For your security, we signed you out - Annual Tax Summary - GOV.UK")
      case "/"                        =>
        ExpectedData(
          "Account home - Personal tax account - GOV.UK"
        )

      case key => throw new RuntimeException(s"Expected data are missing for `$key`")
    }

  val urls: Map[String, ExpectedData] = Map(
    "/annual-tax-summary"                                             -> getExpectedData("/"),
    "/annual-tax-summary/main"                                        -> getExpectedData("main"),
    "/annual-tax-summary/summary"                                     -> getExpectedData("summary"),
    "/annual-tax-summary/nics"                                        -> getExpectedData("nics"),
    "/annual-tax-summary/treasury-spending"                           -> getExpectedData("treasury-spending"),
    "/annual-tax-summary/income-before-tax"                           -> getExpectedData("income-before-tax"),
    "/annual-tax-summary/tax-free-amount"                             -> getExpectedData("tax-free-amount"),
    "/annual-tax-summary/total-income-tax"                            -> getExpectedData("total-income-tax"),
    "/annual-tax-summary/capital-gains-tax"                           -> getExpectedData("capital-gains-tax"),
    //"/annual-tax-summary/not-authorised"                              -> getExpectedData("not-authorised"),
    //"/annual-tax-summary/no-ats"                                      -> getExpectedData("no-ats"),
    //"/annual-tax-summary/service-unavailable"                         -> getExpectedData("service-unavailable"),
    "/annual-tax-summary/sign-out"                                    -> getExpectedData("sign-out"),
    "/annual-tax-summary/paye/treasury-spending/2022"                 -> getExpectedData("paye-year"),
    "/annual-tax-summary/paye/main"                                   -> getExpectedData("paye-main"),
    "/annual-tax-summary/paye/main/2022 "                             -> getExpectedData("paye-main-year"),
    "/annual-tax-summary/upliftConfidenceLevel"                       -> getExpectedData("upliftConfidenceLevel"),
    "/annual-tax-summary/paye/summary/2022"                           -> getExpectedData("paye-summary-year"),
    "/annual-tax-summary/paye/income-tax-and-national-insurance/2022" -> getExpectedData("income-insurance-year"),
    "/annual-tax-summary/paye/income-before-tax/2022"                 -> getExpectedData("income-before-tax-year"),
    "/annual-tax-summary/paye/tax-free-amount/2022"                   -> getExpectedData("tax-free-amount-year"),
    //"/annual-tax-summary/paye/not-authorised"                         -> getExpectedData("not-authorised"),
    //"/annual-tax-summary/paye/service-unavailable"                    -> getExpectedData("paye-service-unavailable"),
    "/annual-tax-summary/keepalive"                                   -> getExpectedData("keepalive"),
    "/annual-tax-summary/session-expired"                             -> getExpectedData("session-expired")
  )

  val messageCount: Int                    = Random.between(1, 100)
  val menuWrapperData: Seq[MenuItemConfig] = Seq(
    MenuItemConfig(
      "home",
      "Account home",
      "/home",
      leftAligned = true,
      position = 0,
      Some("hmrc-account-icon hmrc-account-icon--home"),
      None
    ),
    MenuItemConfig(
      "messages",
      "Messages",
      "/personal-account/messages",
      leftAligned = false,
      position = 0,
      None,
      None
    ),
    MenuItemConfig(
      "progress",
      "progress",
      "/track",
      leftAligned = false,
      position = 1,
      None,
      None
    ),
    MenuItemConfig(
      "profile",
      "Profile and settings",
      "/personal-account/profile-and-settings",
      leftAligned = false,
      position = 2,
      None,
      None
    ),
    MenuItemConfig(
      "signout",
      "Sign out",
      "/personal-account/sign-out",
      leftAligned = false,
      position = 4,
      None,
      None
    )
  )
  val wrapperDataResponse: String          = Json
    .toJson(
      WrapperDataResponse(menuWrapperData, PtaMinMenuConfig("MenuName", "BackName"))
    )
    .toString

  val tcsBrokerUrl = s"/tcs/$generatedNino/dashboard-data"

  val citizenResponse: String =
    s"""|
       |{
        |  "name": {
        |    "current": {
        |      "firstName": "John",
        |      "lastName": "Smith"
        |    },
        |    "previous": []
        |  },
        |  "ids": {
        |    "nino": "$generatedNino"
        |  },
        |  "dateOfBirth": "11121971"
        |}
        |""".stripMargin

  val authResponse: String =
    s"""
       |{
       |    "confidenceLevel": 200,
       |    "nino": "$generatedNino",
       |    "saUtr": "$generatedSaUtr",
       |    "name": {
       |        "name": "John",
       |        "lastName": "Smith"
       |    },
       |    "loginTimes": {
       |        "currentLogin": "2021-06-07T10:52:02.594Z",
       |        "previousLogin": null
       |    },
       |    "optionalCredentials": {
       |        "providerId": "4911434741952698",
       |        "providerType": "GovernmentGateway"
       |    },
       |    "authProviderId": {
       |        "ggCredId": "xyz"
       |    },
       |    "externalId": "testExternalId",
       |    "allEnrolments": [{
       |        "key": "IR-SA-AGENT",
       |        "identifiers": [
       |          {
       |            "key": "IRAgentReference",
       |            "value": "uar"
       |          }],
       |        "state": "Activated"
       |     }]
       |}
       |""".stripMargin

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFeatureFlagService)
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(SCAWrapperToggle))) thenReturn Future.successful(
      FeatureFlag(SCAWrapperToggle, isEnabled = true)
    )
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(PertaxBackendToggle)))
      .thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = false)))

    server.stubFor(
      get(urlPathMatching(s"$cacheMap/.*"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("""
                |{
                |	"id": "session-id",
                |	"data": {
                |   "addressPageVisitedDto": {
                |     "hasVisitedPage": true
                |   }
                |	},
                |	"modifiedDetails": {
                |		"createdAt": {
                |			"$date": 1400258561678
                |		},
                |		"lastUpdated": {
                |			"$date": 1400258561675
                |		}
                |	}
                |}
                |""".stripMargin)
        )
    )

    server.stubFor(
      WireMock
        .get(urlMatching("/single-customer-account-wrapper-data/wrapper-data.*"))
        .willReturn(ok(wrapperDataResponse))
    )

    server.stubFor(
      WireMock
        .get(urlMatching("/single-customer-account-wrapper-data/message-data.*"))
        .willReturn(ok(s"$messageCount"))
    )

    server.stubFor(get(urlEqualTo(s"/citizen-details/nino/$generatedNino")).willReturn(ok(citizenResponse)))

    server.stubFor(
      WireMock
        .get(urlMatching(s"/taxs/$generatedNino/2022/paye-ats-data"))
        .willReturn(ok(FileHelper.loadFile(s"./it/resources/atsData_2022.json")))
    )

    server.stubFor(
      get(
        urlEqualTo(
          s"/taxs/$generatedNino/${appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed}/${appConfig.taxYear}/paye-ats-data"
        )
      )
        .willReturn(ok(FileHelper.loadFile("./it/resources/payeData.json")))
    )

    server.stubFor(
      get(urlEqualTo(s"/taxs//2022/4/ats-list"))
        .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
    )

    //TODO
    server.stubFor(get(urlEqualTo("/messages/count?read=No")).willReturn(ok(s"""{"count": $messageCount}""")))

  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(api.inject.bind[FeatureFlagService].toInstance(mockFeatureFlagService))
    .configure(
      "microservice.services.citizen-details.port"                    -> server.port(),
      "microservice.services.auth.port"                               -> server.port(),
      "microservice.services.tax-summaries.port"                      -> server.port(),
      "microservice.services.message-frontend.port"                   -> server.port(),
      "sca-wrapper.services.single-customer-account-wrapper-data.url" -> s"http://localhost:${server.port()}",
      "microservice.services.cachable.session-cache.port"             -> server.port(),
      "microservice.services.cachable.session-cache.host"             -> "127.0.0.1"
    )
    .build()

  val uuid: String = UUID.randomUUID().toString

  val cacheMap = s"/keystore/pertax-frontend"

  def request(url: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, url).withSession(SessionKeys.sessionId -> uuid, SessionKeys.authToken -> "Bearer 1")

  "/personal-account/" when {
    "calling authenticated pages" must {
      urls.foreach { case (url, expectedData: ExpectedData) =>
        s"pass content checks at url $url" in {
          server.stubFor(post(urlEqualTo("/auth/authorise")).willReturn(ok(authResponse)))
          val result: Future[Result] = route(app, request(url)).get
          val content                = Jsoup.parse(contentAsString(result))

          //status(result) mustBe OK
          content.title() mustBe expectedData.title

          //val govUkBanner = content.getElementsByClass("govuk-phase-banner")
          //govUkBanner.size() mustBe 1
          // govUkBanner.get(0).getElementsByClass("govuk-link").get(0).attr("href") must include(
          //  "http://localhost:9250/contact/beta-feedback-unauthenticated?service=ATS"
          // )

          val accessibilityStatement = content
            .getElementsByClass("govuk-footer__link")
            .asScala
            .toList
            .map(_.attr("href"))
            .filter(_.contains("accessibility-statement"))
            .head
          accessibilityStatement must include(
            "http://localhost:12346/accessibility-statement/annual-tax-summary?referrerUrl"
          )

          /*val urBannerLink = content
            .getElementsByClass("hmrc-user-research-banner__link")
            .get(0)
            .attr("href")
          if (url.equals("/personal-account/child-benefit/home")) {
            urBannerLink mustBe "https://docs.google.com/forms/d/e/1FAIpQLSegbiz4ClGW0XkC1pY3B02ltiY1V79V7ha0jZinECIz_FvSyg/viewform"
          } else {
            urBannerLink mustBe "https://signup.take-part-in-research.service.gov.uk/home?utm_campaign=PTAhomepage&utm_source=Other&utm_medium=other&t=HMRC&id=209"
          }*/

          /*val signoutLink = content
            .getElementsByClass("hmrc-account-menu__link")
            .asScala
            .toList
            .find(_.html().contains("Sign out"))
            .get
            .attr("href")
          signoutLink mustBe "/personal-account/signout?continueUrl=http%3A%2F%2Flocalhost%3A9514%2Ffeedback%2FPERTAX"*/

          //val displayedMessageCount = content.getElementsByClass("hmrc-notification-badge").get(0).text()
          //displayedMessageCount mustBe messageCount.toString

          val menuItems = content.getElementsByClass("hmrc-account-menu")
          for (menuItem <- menuWrapperData)
            menuItems.text() must include(menuItem.text)

          val languageToggle = content.getElementsByClass("hmrc-language-select__list")
          languageToggle.text() must include("English")
          languageToggle.text() must include("Cymraeg")

          val reportIssueText = content.getElementsByClass("hmrc-report-technical-issue").get(0).text()
          val reportIssueLink = content.getElementsByClass("hmrc-report-technical-issue").get(0).attr("href")
          reportIssueText must include("Is this page not working properly? (opens in new tab)")
          reportIssueLink must include("/contact/report-technical-problem")
        }
      }
    }
  }
}
