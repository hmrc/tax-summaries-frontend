package controllers.auth

import config.ApplicationConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.Injecting
import services.CitizenDetailsService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

class SelfAssessmentActionSpec extends UnitSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite with Injecting {

    lazy implicit val ec = inject[ExecutionContext]
    lazy val appConfig = inject[ApplicationConfig]

    val citizenDetailsService = mock[CitizenDetailsService]
    val ninoAuthAction = mock[NinoAuthAction]

    val action = new SelfAssessmentAction(citizenDetailsService, ninoAuthAction, appConfig)

  class Harness(minAuthAction: MinAuthActionImpl, selfAssessmentAction: SelfAssessmentAction) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = minAuthAction andThen selfAssessmentAction { request =>
      Ok(s"utr is ${request.saUtr}")
    }

  "refine" should {
    "do nothing if the utr is present" in {
      action.invokeBlock()
    }
  }
}
