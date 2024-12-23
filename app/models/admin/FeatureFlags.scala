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

package models.admin

import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName

case object PertaxBackendToggle extends FeatureFlagName {
  override val name: String                = "pertax-backend-toggle"
  override val description: Option[String] = Some(
    "Enable/disable pertax backend during auth"
  )
}

case object SelfAssessmentServiceToggle extends FeatureFlagName {
  override val name: String                = "self-assessment-service-toggle"
  override val description: Option[String] = Some(
    "Enable/disable Self-Assessment services"
  )
}

case object PAYEServiceToggle extends FeatureFlagName {
  override val name: String                = "paye-service-toggle"
  override val description: Option[String] = Some(
    "Enable/disable PAYE services"
  )
}
