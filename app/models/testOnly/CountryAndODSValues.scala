/*
 * Copyright 2024 HM Revenue & Customs
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

package models.testOnly

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.BodyWritable

import scala.util.{Failure, Success, Try}

case class CountryAndODSValues(country: String, odsValues: Map[String, String])

object CountryAndODSValues {

  def stringToKeyValuePairs(v: String): Map[String, String] =
    v.split("""\R""")
      .filter(_.nonEmpty)
      .map(_.trim.split("\\s+"))
      .map { t =>
        if (t.length < 2) {
          t(0) -> ""
        } else {
          t(0) -> t(1)
        }

      }
      .toSeq
      .toMap

  def keyValuePairsToString(v: Map[String, String]): String = {
    val newLine = sys.props("line.separator")
    v.foldLeft("") { (c, i) =>
      def concat: String = if (i._2.isEmpty) {
        i._1
      } else {
        i._1 + " " + i._2
      }

      if (c.isEmpty) {
        concat
      } else {
        c + newLine + concat
      }

    }
  }

  def keyValuePairsToEitherSeqODSValue(v: Map[String, String]): Either[Seq[String], Seq[OdsValue]] =
    v.toSeq.foldLeft[Either[Seq[String], Seq[OdsValue]]](Right(Nil)) { (c, i) =>
      (c, Try(BigDecimal(i._2))) match {
        case (Right(_), Success(value)) =>
          c.map(rr => rr :+ OdsValue(i._1, value))
        case (Left(_), Success(_))      => c
        case (Left(_), Failure(_))      => c.swap.map(_ :+ i._1).swap
        case (Right(_), Failure(_))     => Left(Seq(i._1))
      }
    }

  implicit val writes: Writes[CountryAndODSValues] =
    ((__ \ "country").write[String] and
      (__ \ "odsValues").write[Seq[OdsValue]]) { a =>
      keyValuePairsToEitherSeqODSValue(a.odsValues) match {
        case Left(e)             => throw new RuntimeException("Invalid values for fields:" + e.toString)
        case Right(seqODSValues) => (a.country, seqODSValues)
      }

    }

  implicit val reads: Reads[CountryAndODSValues] =
    ((__ \ "country").read[String] and
      (__ \ "odsValues").read[String])((country, odsValues) =>
      CountryAndODSValues(country, stringToKeyValuePairs(odsValues))
    )

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}
