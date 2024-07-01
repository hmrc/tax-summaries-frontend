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

package uk.gov.hmrc.taxSummaries.repositories

import play.api.libs.json.{Json, OFormat}
import repository.{SessionCacheId, SessionCacheRepository}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongo.test.MongoSupport
import utils.BaseSpec

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class SessionCacheRepositorySpec extends BaseSpec with MongoSupport {

  val repository = new SessionCacheRepository(
    mongoComponent,
    collectionName = "CollectionName",
    ttl = 10 second,
    timestampSupport = new CurrentTimestampSupport()
  )

  case class FakeData(field: String)

  object FakeData {
    implicit val formats: OFormat[FakeData] = Json.format[FakeData]
  }

  "data should be cached based on the session id" in {
    val data = FakeData("test")
    val hc = HeaderCarrier(sessionId = Some(SessionId("SessionId-0000")))
    val wrongHc = HeaderCarrier(sessionId = Some(SessionId("SessionId-xxxx")))
    repository.putSession[FakeData](DataKey[FakeData]("testId"), data)(implicitly, hc, implicitly)

    val result = repository.getFromSession(DataKey[FakeData]("testId"))(implicitly, hc).futureValue.get
    val resultWrong = repository.getFromSession(DataKey[FakeData]("testId"))(implicitly, wrongHc).futureValue

    result mustBe data
    resultWrong mustBe None
  }

  "an exception is thrown" when {
    "A session id is not present" in {
      val data = FakeData("test")
      val hc = HeaderCarrier(sessionId = None)

      val result = Try(repository.putSession[FakeData](DataKey[FakeData]("testId"), data)(implicitly, hc, implicitly))

      result match {
        case Success(_) => ???
        case Failure(exception) => exception mustBe a[SessionCacheId.NoSessionException.type]
      }
    }
  }

}
