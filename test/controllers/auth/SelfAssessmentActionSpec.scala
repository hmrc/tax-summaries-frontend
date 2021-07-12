/*
 * Copyright 2021 HM Revenue & Customs
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

class SelfAssessmentActionSpec
    extends UnitSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite with Injecting {

  lazy implicit val ec = inject[ExecutionContext]
  lazy val appConfig = inject[ApplicationConfig]

  val citizenDetailsService = mock[CitizenDetailsService]
  val ninoAuthAction = mock[NinoAuthAction]

  val action = new SelfAssessmentActionImpl(citizenDetailsService, ninoAuthAction, appConfig)

  class Harness(minAuthAction: MinAuthActionImpl, selfAssessmentAction: SelfAssessmentAction)
      extends InjectedController {
    def onPageLoad(): Action[AnyContent] = (minAuthAction andThen selfAssessmentAction) { request =>
      Ok(s"utr is ${request.saUtr}")
    }
  }

  "refine" should {
    "do nothing if the utr is present" in {
      // action.invokeBlock()
    }
  }
}
