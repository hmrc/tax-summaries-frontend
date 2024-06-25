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

package forms.mappings

import models.Enumerable
import play.api.data.FormError
import play.api.data.format.Formatter

import java.text.DecimalFormat
import scala.util.Try
import scala.util.control.Exception.nonFatalCatch

trait Formatters extends Transforms with Constraints {
  private[mappings] val decimalFormat    = new DecimalFormat("0.00")
  private[mappings] val postcodeRegexp   = """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}$"""
  private[mappings] val numericRegexp    = """^-?(\-?)(\d*)(\.?)(\d*)$"""
  private[mappings] val decimalRegexp    = """^-?(\d*\.\d*)$"""
  private[mappings] val intRegexp        = """^-?(\d*)$"""
  private[mappings] val decimal2DPRegexp = """^-?(\d*\.\d{2})$"""

  private[mappings] val optionalStringFormatter: Formatter[Option[String]] =
    new Formatter[Option[String]] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] =
        Right(
          data
            .get(key)
            .map(standardiseText)
            .filter(_.lengthCompare(0) > 0)
        )

      override def unbind(key: String, value: Option[String]): Map[String, String] =
        Map(key -> value.getOrElse(""))
    }

  private[mappings] def stringFormatter(errorKey: String): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(key) match {
          case None | Some("") => Left(Seq(FormError(key, errorKey)))
          case Some(s)         => Right(s)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def booleanFormatter(
    requiredKey: String,
    invalidKey: String,
    invalidPaymentTypeBoolean: String
  ): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true"  => Right(true)
            case "false" => Right(false)
            case "0"     => Left(Seq(FormError(key, invalidPaymentTypeBoolean)))
            case "1"     => Left(Seq(FormError(key, invalidPaymentTypeBoolean)))
            case _       => Left(Seq(FormError(key, invalidKey)))
          }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

  private[mappings] def intFormatter(
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String,
    min: Option[(String, Int)] = None,
    max: Option[(String, Int)] = None,
    args: Seq[String] = Seq.empty
  ): Formatter[Int] =
    new Formatter[Int] {

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .flatMap {
            case s if s.matches(decimalRegexp) =>
              Left(Seq(FormError(key, wholeNumberKey, args)))
            case s                             => nonDecimalIntMatcher(s, key)

          }

      override def unbind(key: String, value: Int): Map[String, String] =
        baseFormatter.unbind(key, value.toString)

      private def nonDecimalIntMatcher(s: String, key: String): Either[Seq[FormError], Int] =
        Try(BigInt(s)).toOption match {
          case Some(l) if min.isDefined && l < min.get._2 => Left(Seq(FormError(key, min.get._1, args)))
          case Some(l) if max.isDefined && l > max.get._2 => Left(Seq(FormError(key, max.get._1, args)))
          case _                                          =>
            nonFatalCatch
              .either(s.toInt)
              .left
              .map(_ => Seq(FormError(key, nonNumericKey, args)))
        }
    }

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String)(implicit
    ev: Enumerable[A]
  ): Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap { str =>
          ev.withName(str).map(Right.apply).getOrElse(Left(Seq(FormError(key, invalidKey))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }
}