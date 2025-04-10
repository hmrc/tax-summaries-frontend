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

package testOnly.controllers

import models.admin.{PAYEServiceToggle, PertaxBackendToggle, SelfAssessmentServiceToggle}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class FeatureFlagsController @Inject() (
  mcc: MessagesControllerComponents,
  featureFlagService: FeatureFlagService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  def setDefaults(): Action[AnyContent] = Action.async {
    featureFlagService
      .setAll(Map(PertaxBackendToggle -> true, SelfAssessmentServiceToggle -> true, PAYEServiceToggle -> true))
      .map(_ => Ok("Default flags set"))
  }
}
