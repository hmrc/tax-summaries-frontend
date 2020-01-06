/*
 * Copyright 2020 HM Revenue & Customs
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

package utils.validation

import play.api.data.validation.Valid
import utils.validation.ConstraintUtil.{FieldFormatConstraintParameter, FieldIsEmptyConstraintParameter, FieldMaxLengthConstraintParameter}
import utils.validation.ErrorMessageFactory._

object ErrorMessagesUtilAPI {

  val simpleErrorMessage = (fieldId: String, msgId: String) =>
    createErrorMessage(
      TargetFieldIds(fieldId),
      FieldErrorConfig(msgId)
  )

  val simpleCrossFieldErrorMessage = (ids: TargetFieldIds, msgId: String) =>
    createErrorMessage(
      ids,
      FieldErrorConfig(msgId)
  )

  def simpleFieldIsEmptyConstraintParameter(fieldId: String, msgId: String): FieldIsEmptyConstraintParameter =
    FieldIsEmptyConstraintParameter(simpleErrorMessage(fieldId, msgId))

  def genericInvalidFormatConstraintParameter(
    validationFunction: (String) => Boolean,
    fieldId: String,
    fieldNameInErrorMessage: String,
    errorMsg: String = "generic.error.character_invalid"): Seq[FieldFormatConstraintParameter] =
    Seq[FieldFormatConstraintParameter](
      FieldFormatConstraintParameter(
        (name: String) =>
          validationFunction(name) match {
            case true =>
              Valid
            case false =>
              createErrorMessage(
                TargetFieldIds(fieldId),
                FieldErrorConfig(errorMsg),
                SummaryErrorConfig(MessageArguments(fieldNameInErrorMessage)))
        }
      )
    )

  def genericFieldMaxLengthConstraintParameter(
    maxLen: Int,
    fieldId: String,
    fieldNameInErrorMessage: String): FieldMaxLengthConstraintParameter =
    FieldMaxLengthConstraintParameter(
      maxLen,
      createErrorMessage(
        TargetFieldIds(fieldId),
        FieldErrorConfig("generic.error.maximum_length", MessageArguments(fieldNameInErrorMessage, maxLen)),
        SummaryErrorConfig(MessageArguments(fieldNameInErrorMessage))
      )
    )
}
