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

package utils

import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait TaxsUnitTestTraits
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite
    with Injecting {

  implicit lazy val hc = HeaderCarrier()

  implicit lazy val ec = inject[ExecutionContext]

  implicit def convertToOption[T](value: T): Option[T] = Some(value)

  implicit def convertToFuture[T](value: T): Future[Option[T]] = Future.successful(value)

  implicit def convertToFuture[T](err: Throwable): Future[Option[T]] = Future.failed(err)

  // used to help mock setup functions to clarify if certain results must be mocked.
  sealed trait MockConfiguration[+A] {
    final def get = this match {
      case Configure(config) => config
      case _                 => throw new RuntimeException("This element is not to be configured")
    }

    final def ifConfiguredThen(action: A => Unit): Unit = this match {
      case Configure(dataToReturn) => action(dataToReturn)
      case _                       =>
    }
  }

  case class Configure[A](config: A) extends MockConfiguration[A]

  case object DoNotConfigure extends MockConfiguration[Nothing]

  implicit def convertToMockConfiguration[T](value: T): MockConfiguration[T] = Configure(value)

  implicit def convertToMockConfiguration2[T](value: T): MockConfiguration[Option[T]] = Configure(value)

  implicit def convertToMockConfiguration3[T](value: T): MockConfiguration[Future[T]] = Configure(Future(value))

  implicit def convertToMockConfiguration4[T](value: T): MockConfiguration[Future[Option[T]]] =
    Configure(Future(Some(value)))

  implicit def convertToMockConfiguration5[T](err: Throwable): MockConfiguration[Future[Option[T]]] = Configure(err)

  // used to for mock setup functions to specify the location of the data,
  // CachedLocally for when the data can be found in KeyStore or Save4Later, and NotCachedLocally where
  // a call to another service is required
  sealed trait CacheConfigurationLocation

  case object CachedLocally extends CacheConfigurationLocation

  case object NotCachedLocally extends CacheConfigurationLocation

  implicit class VerificationUtil(someCount: Option[Int]) {
    // util function designed for aiding verify functions
    def ifDefinedThen(action: (Int) => Unit) = someCount match {
      case Some(count) => action(count)
      case _           =>
    }
  }

  case class Ids(utr: Boolean, nino: Boolean, crn: Boolean, vrn: Boolean)

  def testId(mustExist: Boolean)(targetFieldId: String)(implicit doc: Document) = mustExist match {
    case false => doc.getElementById(targetFieldId) mustBe null
    case true  => doc.getElementById(targetFieldId) must not be null
  }

  def testText(expectedText: String)(targetFieldId: String)(implicit doc: Document) =
    doc.getElementById(targetFieldId).text mustBe expectedText

}
