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

package test.utils

import play.api.i18n.Messages
import utils.validation.{FieldError, MessageArguments, SummaryError}
import models.TupleDate

trait ImplicitSingleFieldTestAPI {
  implicit val fieldId: String
}

trait ImplicitCrossFieldTestAPI {
  implicit val fieldIds: Set[String]
}

trait FormValidationTestAPI {}

trait ExpectedErrorExpectation {
  def fieldError: FieldError
  def summaryError: SummaryError
}

case class ExpectedFieldIsEmpty(val fieldError: FieldError, val summaryError: SummaryError)
    extends ExpectedErrorExpectation

object ExpectedFieldIsEmpty {
  def apply(anchorId: String, fieldError: FieldError)(implicit messages: Messages): ExpectedFieldIsEmpty =
    new ExpectedFieldIsEmpty(fieldError, SummaryError(fieldError, anchorId))
}

case class ExpectedFieldExceedsMaxLength(val fieldError: FieldError, val summaryError: SummaryError, val maxLength: Int)
    extends ExpectedErrorExpectation

object ExpectedFieldExceedsMaxLength {
  // quick constructor for the default expected max length error messages
  def apply(fieldId: String, embeddedFieldNameInErrorMessages: String, maxLen: Int)(
    implicit messages: Messages): ExpectedFieldExceedsMaxLength = {
    val defaultKey = "awrs.generic.error.maximum_length"
    val defaultError = FieldError(defaultKey, MessageArguments(embeddedFieldNameInErrorMessages, maxLen))
    new ExpectedFieldExceedsMaxLength(
      defaultError,
      SummaryError(defaultError, MessageArguments(embeddedFieldNameInErrorMessages), fieldId),
      maxLen)
  }
}

sealed trait MaxLengthOption[+A] {
  def toOption = this match {
    case MaxLengthDefinition(maxLength) => Some(maxLength)
    case MaxLengthIsHandledByTheRegEx() => None
  }

  def nonEmpty: Boolean = this match {
    case MaxLengthDefinition(_)         => true
    case MaxLengthIsHandledByTheRegEx() => false
  }

  def isEmpty: Boolean = this match {
    case MaxLengthDefinition(_)         => false
    case MaxLengthIsHandledByTheRegEx() => true
  }
}

case class MaxLengthDefinition[A <: ExpectedFieldExceedsMaxLength](val get: A) extends MaxLengthOption[A]

case class MaxLengthIsHandledByTheRegEx() extends MaxLengthOption[Nothing]

case class ExpectedInvalidFieldFormat(
  val invalidCase: String,
  val fieldError: FieldError,
  val summaryError: SummaryError)
    extends ExpectedErrorExpectation

object ExpectedInvalidFieldFormat {
  def apply(invalidCase: String, fieldId: String, embeddedFieldNameInErrorMessages: String)(
    implicit messages: Messages): ExpectedInvalidFieldFormat = {
    val defaultKey = "awrs.generic.error.character_invalid"
    val defaultFieldError = FieldError(defaultKey)
    val defaultSummaryError =
      SummaryError(defaultFieldError, MessageArguments(embeddedFieldNameInErrorMessages), fieldId)
    new ExpectedInvalidFieldFormat(invalidCase, defaultFieldError, defaultSummaryError)
  }

  def apply(invalidCase: String, fieldId: String, fieldError: FieldError)(
    implicit messages: Messages): ExpectedInvalidFieldFormat = {
    val defaultSummaryError = SummaryError(fieldError, fieldId)
    new ExpectedInvalidFieldFormat(invalidCase, fieldError, defaultSummaryError)
  }
}

case class ExpectedValidFieldFormat(val validCase: String)

case class ExpectedFieldFormat(
  val invalidFormats: List[ExpectedInvalidFieldFormat],
  val validFormats: List[ExpectedValidFieldFormat] = List[ExpectedValidFieldFormat]())

case class CompulsoryFieldValidationExpectations(
  val fieldIsEmptyExpectation: ExpectedFieldIsEmpty,
  val maxLengthExpectation: MaxLengthOption[ExpectedFieldExceedsMaxLength],
  val formatExpectations: ExpectedFieldFormat) {
  def toOptionalFieldValidationExpectations: OptionalFieldValidationExpectations =
    new OptionalFieldValidationExpectations(maxLengthExpectation, formatExpectations)

  def toFieldToIgnore: FieldToIgnore =
    new FieldToIgnore(maxLengthExpectation match {
      case MaxLengthDefinition(maxLength) => Option(maxLength.maxLength)
      case _                              => None
    }, formatExpectations)
}

object CompulsoryFieldValidationExpectations {
  def apply(
    fieldIsEmptyExpectation: ExpectedFieldIsEmpty,
    maxLengthExpectation: ExpectedFieldExceedsMaxLength,
    formatExpectations: ExpectedFieldFormat) =
    new CompulsoryFieldValidationExpectations(
      fieldIsEmptyExpectation,
      MaxLengthDefinition(maxLengthExpectation),
      formatExpectations)
}

case class OptionalFieldValidationExpectations(
  val maxLengthExpectation: MaxLengthOption[ExpectedFieldExceedsMaxLength],
  val formatExpectations: ExpectedFieldFormat) {
  def toFieldToIgnore: FieldToIgnore =
    new FieldToIgnore(maxLengthExpectation match {
      case MaxLengthDefinition(maxLength) => Option(maxLength.maxLength)
      case _                              => None
    }, formatExpectations)
}

object OptionalFieldValidationExpectations {
  def apply(maxLengthExpectation: ExpectedFieldExceedsMaxLength, formatExpectations: ExpectedFieldFormat) =
    new OptionalFieldValidationExpectations(MaxLengthDefinition(maxLengthExpectation), formatExpectations)
}

case class CompulsoryEnumValidationExpectations(
  val fieldIsEmptyExpectation: ExpectedFieldIsEmpty,
  val validEnumValues: Set[Enumeration#Value],
  val invalidEnumValues: Set[Enumeration#Value]) {
  def toIgnoreEnumFieldExpectation: EnumFieldToIgnore =
    new EnumFieldToIgnore(fieldIsEmptyExpectation, validEnumValues, invalidEnumValues)
}

object CompulsoryEnumValidationExpectations {
  private val empty: Set[Enumeration#Value] = Set[Enumeration#Value]()

  def apply(fieldIsEmptyExpectation: ExpectedFieldIsEmpty, expectedEnum: Enumeration) =
    new CompulsoryEnumValidationExpectations(fieldIsEmptyExpectation, expectedEnum.values.toSet, empty)

  //TODO add constructor to auto add unused enum#values from the enum to the ignore list
}

case class ExpectedValidDateFormat(val validCase: TupleDate)

case class ExpectedInvalidDateFormat(
  val invalidCase: TupleDate,
  val fieldError: FieldError,
  val summaryError: SummaryError)
    extends ExpectedErrorExpectation

case class ExpectedDateFormat(
  val invalidFormats: List[ExpectedInvalidDateFormat],
  val validFormats: List[ExpectedValidDateFormat] = List[ExpectedValidDateFormat]())

case class CompulsoryDateValidationExpectations(
  val fieldIsEmptyExpectation: ExpectedFieldIsEmpty,
  val formatExpectations: ExpectedDateFormat) {
  def toDateToIgnore: DateToIgnore = new DateToIgnore(None, formatExpectations)
}
case class DateToIgnore(val maxLength: Option[Int], val formatExpectations: ExpectedDateFormat)

case class FieldToIgnore(val maxLength: Option[Int], val formatExpectations: ExpectedFieldFormat)

case class EnumFieldToIgnore(
  val fieldIsEmptyExpectation: ExpectedFieldIsEmpty,
  val validEnumValues: Set[Enumeration#Value],
  val invalidEnumValues: Set[Enumeration#Value])

object EnumFieldToIgnore {
  private val empty: Set[Enumeration#Value] = Set[Enumeration#Value]()

  def apply(fieldIsEmptyExpectation: ExpectedFieldIsEmpty, expectedEnum: Enumeration) =
    new EnumFieldToIgnore(fieldIsEmptyExpectation, expectedEnum.values.toSet, empty)
}

case class CrossFieldValidationExpectations(val anchor: String, val fieldIsEmptyExpectation: ExpectedFieldIsEmpty)

/**
  * A class used to specify prefix for ids
  *
  * It's intended to be used by FieldNameUtilAPI and ImplicitFieldNameUtil
  * to easily attach prefix to ids when it is abscent or supplied
  *
  * @param prefix
  */
case class IdPrefix(val prefix: Option[String])

/**
  * implicit conversions from String and Option[String] to IdPrefix
  */
object IdPrefix {
  def apply(str: String): IdPrefix = new IdPrefix(Some(str))

  implicit def fromString(str: String): IdPrefix = new IdPrefix(Some(str))

  implicit def fromString(str: Option[String]): IdPrefix = new IdPrefix(str)
}

/**
  * function designed to be used by ImplicitFieldNameUtil
  * to allow easy attachment of prefix to field ids regardless of whether the
  * prefix is supplied
  */
trait FieldNameUtilAPI {
  def attach(fieldId: String): String

  def attachToAll(fieldIds: Set[String]): Set[String]
}
