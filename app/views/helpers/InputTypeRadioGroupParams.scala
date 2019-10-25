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

package views.helpers

import play.api.data.Field
import play.twirl.api.Html

case class InputTypeRadioGroupParams(
  field: Field,
  radioOptions: Seq[(String, String)],
  inputId: Option[String] = None,
  inputClass: Option[String] = None,
  legend: Option[String] = None,
  legendClass: Option[String] = None,
  legendId: Option[String] = None,
  legendAttributes: Option[String] = None,
  fieldSetClass: Option[String] = None,
  fieldSetAttributes: Option[String] = None,
  fieldSetQuestion: Option[String] = None,
  fieldSetQuestionId: Option[String] = None,
  fieldSetQuestionWrapperClass: Option[String] = None,
  fieldSetQuestionAnswer: Option[Html] = None,
  labelClass: Option[String] = None,
  labelAfter: Boolean = false,
  labelStacked: Boolean = false,
  wrapperClass: Option[String] = None,
  formHint: Option[String] = None,
  formHintId: Option[String] = None,
  dataAttributes: Option[String] = None,
  optionMessages: Option[List[(String, String)]] = None,
  ariaDescribedBy: Option[String] = None,
  ariaDescribedByForYesOptionOnly: Boolean = false,
  formHasErrors: Boolean = false)
