package views


import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, urlEqualTo}
import connectors.DataCacheConnector
import org.mockito.MockitoSugar
import play.api
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, writeableOf_AnyContentAsEmpty, status => getStatus}
import testUtils.{FileHelper, IntegrationSpec}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import uk.gov.hmrc.scalatestaccessibilitylinter.domain.OutputFormat

import java.util.UUID
import scala.concurrent.Future

class a11yTestSpec extends IntegrationSpec with AccessibilityMatchers with MockitoSugar {

  lazy val backendUrl = s"/taxs/$generatedSaUtr/$fakeTaxYear/ats-data"
  lazy val backendUrlSa = s"/taxs/$generatedSaUtr/ats-list"

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port"          -> server.port(),
      "microservice.services.tax-summaries.port" -> server.port(),
      "microservice.services.cachable.session-cache.port" -> server.port(),
      "microservice.services.pertax.port" -> server.port()
    ).overrides(
    api.inject.bind[DataCacheConnector].toInstance(mockDataCacheConnector),
      api.inject.bind[AsyncCacheApi].toInstance(mock[AsyncCacheApi])
  )
    .build()

  def request(url: String): FakeRequest[AnyContentAsEmpty.type] = {
    val uuid = UUID.randomUUID().toString
    FakeRequest(GET, url).withSession(SessionKeys.sessionId -> uuid, SessionKeys.authToken -> "Bearer 1")

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
        server.stubFor(
          get(urlEqualTo(s"/pertax/$generatedNino/authorise"))
            .willReturn(ok("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
        )

        val result: Future[Result] = route(app, request(url)).get
        getStatus(result) mustBe OK
        contentAsString(result) must passAccessibilityChecks(OutputFormat.Verbose)
      }
    }
  }

  "annual-tax-summary data pages" must {
    List(
      s"/annual-tax-summary/main?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/summary?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/nics?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/treasury-spending?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/income-before-tax?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/tax-free-amount?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/total-income-tax?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/capital-gains-tax?taxYear=$fakeTaxYear",
    ).foreach { url =>
      s"pass accessibility validation at url $url" in {
        server.stubFor(
          get(urlEqualTo(backendUrlSa))
            .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
        )

        server.stubFor(
          get(urlEqualTo(backendUrl))
            .willReturn(ok(FileHelper.loadFile(s"./it/resources/atsData_$fakeTaxYear.json")))
        )

        server.stubFor(
          get(urlEqualTo(s"/pertax/$generatedNino/authorise"))
            .willReturn(ok("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
        )

        val result: Future[Result] = route(app, request(url)).get
        println("aaaaaaa" + contentAsString(result))
        getStatus(result) mustBe OK
        contentAsString(result) must passAccessibilityChecks(OutputFormat.Verbose)
      }
    }
  }
}
