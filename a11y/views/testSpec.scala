package views


import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, post, urlEqualTo}
import connectors.DataCacheConnector
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, redirectLocation, route, writeableOf_AnyContentAsEmpty, status => getStatus}
import services.SummaryService
import testUtils.{FileHelper, IntegrationSpec}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import uk.gov.hmrc.scalatestaccessibilitylinter.domain.OutputFormat
import testUtils.FileHelper
import utils.GenericViewModel
import utils.TestConstants.mock

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class testSpec extends IntegrationSpec with AccessibilityMatchers {

  lazy val backendUrl = s"/taxs/$generatedSaUtr/$taxYear/ats-data"
  lazy val backendUrlSa = s"/taxs/$generatedSaUtr/ats-list"

  lazy val backendUrlPaye =
    s"/taxs/$generatedNino/${appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed}/${appConfig.taxYear}/paye-ats-data"

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port"          -> server.port(),
      "microservice.services.tax-summaries.port" -> server.port(),
      "microservice.services.cachable.session-cache.port" -> server.port()
    ).overrides(
    api.inject.bind[DataCacheConnector].toInstance(mockDataCacheConnector)
  )
    .build()

  def request(url: String): FakeRequest[AnyContentAsEmpty.type] = {
    val uuid = UUID.randomUUID().toString
    FakeRequest(GET, url).withSession(SessionKeys.sessionId -> uuid)
  }

  "annual-tax-summary" must {
    List(
      "/annual-tax-summary/",
      "/annual-tax-summary/paye/main"
    ).foreach { url =>
      s"pass accessibility validation at url $url" in {
        server.stubFor(
          get(urlEqualTo(backendUrlSa))
            .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
        )

        val result: Future[Result] = route(app, request(url)).get
        getStatus(result) mustBe OK
        contentAsString(result) must passAccessibilityChecks(OutputFormat.Verbose)
      }
    }
  }

  "annual-tax-summary data pages" must {
    List(
      s"/annual-tax-summary/main?taxYear=$taxYear",
      s"/annual-tax-summary/summary?taxYear=$taxYear",
      s"/annual-tax-summary/nics?taxYear=$taxYear",
      s"/annual-tax-summary/treasury-spending?taxYear=$taxYear",
      s"/annual-tax-summary/income-before-tax?taxYear=$taxYear",
      s"/annual-tax-summary/tax-free-amount?taxYear=$taxYear",
      s"/annual-tax-summary/total-income-tax?taxYear=$taxYear",
      s"/annual-tax-summary/capital-gains-tax?taxYear=$taxYear",
    ).foreach { url =>
      s"pass accessibility validation at url $url" in {
        server.stubFor(
          get(urlEqualTo(backendUrlSa))
            .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
        )

        server.stubFor(
          get(urlEqualTo(backendUrl))
            .willReturn(ok(FileHelper.loadFile(s"./it/resources/atsData_$taxYear.json")))
        )

        val result: Future[Result] = route(app, request(url)).get
        getStatus(result) mustBe OK
        contentAsString(result) must passAccessibilityChecks(OutputFormat.Verbose)
      }
    }
  }
}
