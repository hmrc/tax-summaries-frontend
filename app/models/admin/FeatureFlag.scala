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

import models.admin.FeatureFlagName.allFeatureFlags
import play.api.libs.json._
import play.api.mvc.PathBindable
import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class FeatureFlag(name: FeatureFlagName, isEnabled: Boolean, description: Option[String] = None)

object FeatureFlag {
  implicit val format: OFormat[FeatureFlag] = Json.format[FeatureFlag]
}

sealed trait FeatureFlagName {
  val description: Option[String] = None
}

object FeatureFlagName {
  implicit val writes: Writes[FeatureFlagName] = new Writes[FeatureFlagName] {
    override def writes(o: FeatureFlagName): JsValue = JsString(o.toString)
  }

  implicit val reads: Reads[FeatureFlagName] = new Reads[FeatureFlagName] {
    override def reads(json: JsValue): JsResult[FeatureFlagName] =
      allFeatureFlags
        .find(flag => JsString(flag.toString) == json)
        .map(JsSuccess(_))
        .getOrElse(JsError(s"Unknown FeatureFlagName `${json.toString}`"))
  }

  implicit val formats: Format[FeatureFlagName] =
    Format(reads, writes)

  implicit def pathBindable: PathBindable[FeatureFlagName] = new PathBindable[FeatureFlagName] {

    override def bind(key: String, value: String): Either[String, FeatureFlagName] =
      JsString(value).validate[FeatureFlagName] match {
        case JsSuccess(name, _) =>
          Right(name)
        case _                  =>
          Left(s"The feature flag `$value` does not exist")
      }

    override def unbind(key: String, value: FeatureFlagName): String =
      value.toString
  }

  val allFeatureFlags = List(PertaxBackendToggle)
}

case object PertaxBackendToggle extends FeatureFlagName {
  override def toString: String            = "pertax-backend-toggle"
  override val description: Option[String] = Some(
    "Enable/disable pertax backend during auth"
  )
}

case class DeletedToggle(name: String) extends FeatureFlagName {
  override def toString: String = name
}

object FeatureFlagMongoFormats {
  val featureFlagNameReads: Reads[FeatureFlagName] = new Reads[FeatureFlagName] {
    override def reads(json: JsValue): JsResult[FeatureFlagName] =
      allFeatureFlags
        .find(flag => JsString(flag.toString) == json)
        .map(JsSuccess(_))
        .getOrElse(JsSuccess(DeletedToggle(json.as[String])))
  }

  val featureFlagReads: Reads[FeatureFlag] = ((JsPath \ "name").read[FeatureFlagName](featureFlagNameReads) and
    (JsPath \ "isEnabled").read[Boolean] and
    (JsPath \ "description").readNullable[String])(FeatureFlag.apply _)

  val writes: Writes[FeatureFlagName] = (o: FeatureFlagName) => JsString(o.toString)

  val formats: Format[FeatureFlag] = Json.format[FeatureFlag]
}
