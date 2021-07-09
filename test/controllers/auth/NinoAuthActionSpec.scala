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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import uk.gov.hmrc.auth.core.InsufficientConfidenceLevel
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class NinoAuthActionSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with Injecting with ScalaFutures {

  implicit val hc = HeaderCarrier()
  lazy implicit val ec = inject[ExecutionContext]
  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]

  val ninoAuthAction = new NinoAuthAction(mockAuthConnector)

  "getNino" should {
    "return a NINO when auth returns a NINO" in {
      val nino = new Generator().nextNino.nino
      val retrievalResult: Future[Option[String]] =
        Future.successful(Some(nino))

      when(
        mockAuthConnector
          .authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      ninoAuthAction.getNino().futureValue shouldBe SuccessAtsNino(nino)
    }

    "return no NINO when auth returns no NINO" in {
      val retrievalResult: Future[Option[String]] =
        Future.successful(None)

      when(
        mockAuthConnector
          .authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      ninoAuthAction.getNino().futureValue shouldBe NoAtsNinoFound
    }

    "return an InsufficientConfidenceLevel Response when the user needs to uplift" in {
      val retrievalResult: Future[Option[String]] =
        Future.failed(new InsufficientConfidenceLevel)

      when(
        mockAuthConnector
          .authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      ninoAuthAction.getNino().futureValue shouldBe UpliftRequiredAtsNino
    }
  }
}
