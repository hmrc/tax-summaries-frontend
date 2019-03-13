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

import org.scalatest.Suite
import play.api.test.FakeApplication
import uk.gov.hmrc.play.test.WithFakeApplication

trait FakeTaxsPlayApplication  extends WithFakeApplication {
  this: Suite =>

  val config : Map[String, _] = Map(
    "Test.microservice.services.contact-frontend.host" -> "localhost",
    "Test.microservice.services.contact-frontend.port" -> "9250",
    "Test.microservice.services.auditing.host" -> "localhost",
    "Test.microservice.services.auditing.port" -> "8100"
  )

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)
}
