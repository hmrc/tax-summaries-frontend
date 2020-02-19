package controllers.paye

import connectors.MiddleConnector
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import services.PayeAtsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil

class GovernmentSpendControllerSpec  extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with IntegrationPatience {

  implicit val hc = HeaderCarrier()

  class TestController extends PayeGovernmentSpendController {
    lazy val payeAtsService: PayeAtsService = mock[PayeAtsService]
  }

  "Government spend controller" should {

    "return OK and a correct view for a GET" in new TestController {


    }

    "return bad request and errors when receiving any errors from service" in new TestController {


    }
  }

}
