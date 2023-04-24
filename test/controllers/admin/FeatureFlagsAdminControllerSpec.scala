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

package controllers.admin

import config.ApplicationConfig
import controllers.auth.InternalAuthAction
import models.admin.{FeatureFlag, PertaxBackendToggle}
import org.mockito.ArgumentMatchers.any
import play.api.Configuration
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsBoolean, JsResultException, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, defaultAwaitTimeout, status}
import services.admin.FeatureFlagService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client.{IAAction, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.BaseSpec

import scala.concurrent.Future

class FeatureFlagsAdminControllerSpec extends BaseSpec {
  implicit val cc        = inject[ControllerComponents]
  val expectedPermission =
    Permission(
      resource = Resource(
        resourceType = ResourceType("ddcn-live-admin-frontend"),
        resourceLocation = ResourceLocation("*")
      ),
      action = IAAction("ADMIN")
    )

  lazy val mockStubBehaviour: StubBehaviour           = mock[StubBehaviour]
  lazy val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]
  lazy val fakeInternalAuthAction                     =
    new InternalAuthAction(
      new ApplicationConfig(inject[ServicesConfig], inject[Configuration]),
      BackendAuthComponentsStub(mockStubBehaviour)
    )

  val sut = new FeatureFlagsAdminController(fakeInternalAuthAction, mockFeatureFlagService, cc)(ec)

  override def beforeEach(): Unit = {
    reset(mockStubBehaviour)
    reset(mockFeatureFlagService)
    when(mockStubBehaviour.stubAuth(Some(expectedPermission), Retrieval.username))
      .thenReturn(Future.successful(Retrieval.Username("User name")))
  }

  "GET /get" must {
    "returns a list of toggles" when {
      "all is well" in {
        when(mockFeatureFlagService.getAll).thenReturn(Future.successful(List(FeatureFlag(PertaxBackendToggle, true))))

        val result = sut.get(
          FakeRequest().withHeaders("Authorization" -> "Token some-token")
        )

        status(result) mustBe OK
        contentAsString(result) mustBe """[{"name":"pertax-backend-toggle","isEnabled":true}]"""
      }
    }

    "returns an exception" when {
      "The user is not authorised" in {
        when(mockFeatureFlagService.getAll).thenReturn(Future.successful(List(FeatureFlag(PertaxBackendToggle, true))))
        reset(mockStubBehaviour)
        when(mockStubBehaviour.stubAuth(Some(expectedPermission), Retrieval.username))
          .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", Status.UNAUTHORIZED)))

        val result = sut.get(
          FakeRequest().withHeaders("Authorization" -> "Token some-token")
        )

        whenReady(result.failed) { e =>
          e mustBe a[UpstreamErrorResponse]
        }
      }
    }
  }

  "PUT /put" must {
    "returns no content" when {
      "all is well" in {
        when(mockFeatureFlagService.set(any(), any())).thenReturn(Future.successful(true))

        val result = sut.put(PertaxBackendToggle)(
          FakeRequest()
            .withHeaders("Authorization" -> "Token some-token")
            .withJsonBody(JsBoolean(true))
        )

        status(result) mustBe NO_CONTENT
        contentAsString(result) mustBe ""
      }
    }
  }

  "PUT /puAll" must {
    "returns no content" when {
      "all is well" in {
        when(mockFeatureFlagService.setAll(any())).thenReturn(Future.successful(()))

        val result = sut.putAll(
          FakeRequest()
            .withHeaders("Authorization" -> "Token some-token")
            .withJsonBody(Json.toJson(List(FeatureFlag(PertaxBackendToggle, true))))
        )

        status(result) mustBe NO_CONTENT
      }
    }

    "returns internal server error" when {
      "there is an error" in {
        when(mockFeatureFlagService.setAll(any())).thenReturn(Future.failed(new RuntimeException("Random exception")))

        val result = intercept[RuntimeException] {
          await(
            sut.putAll(
              FakeRequest()
                .withHeaders("Authorization" -> "Token some-token")
                .withJsonBody(Json.toJson(List(FeatureFlag(PertaxBackendToggle, true))))
            )
          )
        }
        result.getMessage mustBe "Random exception"
      }

      "Toggle in json body does not exist" in {
        when(mockFeatureFlagService.setAll(any())).thenReturn(Future.successful(()))

        val result = sut.putAll(
          FakeRequest()
            .withHeaders("Authorization" -> "Token some-token")
            .withJsonBody(Json.parse("""[{"name":"non-existent-toggle","isEnabled":true}]"""))
        )

        whenReady(result.failed) { ex =>
          ex mustBe a[JsResultException]
          ex.getMessage must include("""Unknown FeatureFlagName `"non-existent-toggle"`""")
        }
      }
    }
  }
}
