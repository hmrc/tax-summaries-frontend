package models

import play.api.libs.json.{Json, OFormat}

case class PertaxApiResponse(
                        code: String,
                        message: String,
                        errorView: Option[ErrorView] = None,
                        redirect: Option[String] = None)

object PertaxApiResponse {
  implicit val format: OFormat[PertaxApiResponse] = Json.format[PertaxApiResponse]
}
