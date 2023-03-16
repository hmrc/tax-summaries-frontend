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

package repositories.admin

import config.ApplicationConfig
import models.admin.{FeatureFlag, FeatureFlagName, PertaxBackendToggle}
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.scalatest.time.{Millis, Seconds, Span}
import play.api
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import utils.BaseSpec

import scala.concurrent.Future

class FeatureFlagRepositorySpec extends BaseSpec with DefaultPlayMongoRepositorySupport[FeatureFlag] {

  override protected lazy val optSchema: Option[BsonDocument] = Some(BsonDocument("""
      { bsonType: "object"
      , required: [ "_id", "name", "isEnabled" ]
      , properties:
        { _id       : { bsonType: "objectId" }
        , name      : { bsonType: "string" }
        , isEnabled : { bsonType: "bool" }
        , description : { bsonType: "string" }
        }
      }
    """))

  def insertRecord(name: FeatureFlagName = PertaxBackendToggle, enabled: Boolean = true): Future[Boolean] =
    insert(FeatureFlag(name, enabled))
      .map(_.wasAcknowledged())

  override def beforeEach(): Unit = {
    dropCollection()
    super.beforeEach()
  }

  lazy val config = mock[ApplicationConfig]

  override implicit lazy val app = GuiceApplicationBuilder()
    .overrides(api.inject.bind[ApplicationConfig].toInstance(config))
    .configure(Map("mongodb.uri" -> mongoUri))
    .build()

  lazy val repository = inject[FeatureFlagRepository]

  override val checkTtlIndex = false

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "getFlag" must {
    "return None if there is no record" in {
      val result = repository.getFeatureFlag(PertaxBackendToggle).futureValue

      result mustBe None
    }
  }

  "setFeatureFlag and getFeatureFlag" must {
    "insert and read a record in mongo" in {
      val result = (for {
        _      <- insertRecord(PertaxBackendToggle, true)
        result <- repository.getFeatureFlag(PertaxBackendToggle)
      } yield result).futureValue

      result mustBe Some(FeatureFlag(PertaxBackendToggle, true))

    }
  }

  "setFeatureFlag" must {
    "replace a record not create a new one" in {
      val result = (for {
        _      <- repository.setFeatureFlag(PertaxBackendToggle, true)
        _      <- repository.setFeatureFlag(PertaxBackendToggle, false)
        result <- find(Filters.equal("name", PertaxBackendToggle.toString))
      } yield result).futureValue

      result.length mustBe 1
      result.head.isEnabled mustBe false
    }
  }

  "setFeatureFlags" must {
    "replace multiple records and not create a new one" in {
      val result = (for {
        _      <- insertRecord(PertaxBackendToggle, true)
        _      <- repository.setFeatureFlags(
                    Map(
                      PertaxBackendToggle -> false
                    )
                  )
        result <- find(Filters.empty())
      } yield result).futureValue

      result.length mustBe 1
      result.filter(_.name == PertaxBackendToggle).head.isEnabled mustBe false
    }
  }

  "getAllFeatureFlags" must {
    "get a list of all the feature toggles" in {
      val allFlags: Seq[FeatureFlag] = (for {
        _      <- insertRecord(PertaxBackendToggle, true)
        result <- repository.getAllFeatureFlags
      } yield result).futureValue

      allFlags.toSet mustBe List(FeatureFlag(PertaxBackendToggle, true)).toSet
    }
  }

  "Collection" must {
    "not allow duplicates" in {

      val result = intercept[MongoWriteException] {
        await((for {
          _ <- insertRecord(PertaxBackendToggle, true)
          _ <- insertRecord(PertaxBackendToggle, false)
        } yield true))
      }
      result.getCode mustBe 11000
      result.getError.getMessage mustBe s"""E11000 duplicate key error collection: $databaseName.admin-feature-flags index: name dup key: { name: "$PertaxBackendToggle" }"""
    }
  }
}
