/*
 * Copyright 2022 HM Revenue & Customs
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

package models

sealed trait AtsResponse

sealed trait AtsSuccessResponse extends AtsResponse

case object AtsSuccessResponse extends AtsSuccessResponse

case class AtsSuccessResponseWithPayload[T](payload: T) extends AtsSuccessResponse

sealed trait AtsFailureResponse extends AtsResponse {
  val message: String
}

case class AtsNotFoundResponse(message: String) extends AtsFailureResponse
case class AtsErrorResponse(message: String) extends AtsFailureResponse
