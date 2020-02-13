package connectors
import com.codahale.metrics.Timer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import models._
import uk.gov.hmrc.http._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import config.WSHttp
import uk.gov.hmrc.play.test.UnitSpec
import utils.{TestConstants, WireMockHelper}

class MiddleConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with WireMockHelper with MockitoSugar {


  lazy val nino = TestConstants.testNino
  lazy val connector = new MiddleConnector {
    override def http: HttpGet = WSHttp
    override def serviceUrl: String = s"http://localhost:${server.port}"
  }


  implicit val hc = HeaderCarrier()


  val taxYear = 2019

  val expectedAts = AtsData(
    taxYear = 2019,
    utr = None,
    nino = Some("AW843651A"),
    income_tax = None,
    summary_data = None,
    income_data = None,
    allowance_data = None,
    capital_gains_data = None,
    gov_spending = None,
    taxPayerData = None,
    errors = None
  )

  lazy val url =  s"/taxs/$nino/$taxYear/paye-ats-data"

  "retrieve PAYE ATS" should {
    "return ATS Response" in {

      server.stubFor(
        get(urlEqualTo(url))
         .willReturn(ok(Json.toJson[AtsData](expectedAts).toString()))
      )

        val result = await(connector.connectToPayeAts(Nino(nino), taxYear))

      result shouldBe expectedAts
    }
    "Throw NotFoundException on receiving 404 Not Found" in {
      server.stubFor(
        get(urlEqualTo(url))
          
          .willReturn(aResponse().withStatus(404)))
      assertThrows[NotFoundException] {
         val result = await(connector.connectToPayeAts(Nino("AW843651A"), 2019))
       }
    }
    "Throw Upstream5xxResponse on receiving 500 Internal Server Error" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse().withStatus(500)))
      assertThrows[Upstream5xxResponse] {
        val result = await(connector.connectToPayeAts(Nino(nino), taxYear))
      }
    }
  }
}
