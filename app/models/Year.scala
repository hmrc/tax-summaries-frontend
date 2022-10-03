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

package utils

import play.api.libs.json._
import play.api.mvc.{QueryStringBindable, Request}

import scala.util.{Failure, Success, Try}

case class Year(year: Int)

object Year {

  implicit val writes: Writes[Year] = new Writes[Year] {
    override def writes(o: Year): JsValue = JsNumber(o.year)
  }

  implicit val reads: Reads[Year] = new Reads[Year] {
    override def reads(json: JsValue): JsResult[Year] =
      json match {
        case JsNumber(value) =>
          println("3" * 100)
          println(json)
          println("3" * 100)
          JsSuccess(
            Year(value.toInt)
          )
        case _ =>
          println("4" * 100)
          JsError("Not a year") // TODO - Change
      }
  }

  implicit val formats: Format[Year] =
    Format(reads, writes)

  implicit def queryBinder(implicit stringBinder: QueryStringBindable[Int]): QueryStringBindable[Year] =
    new QueryStringBindable[Year] {
      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Year]] = {

        def withParameter(s: Int): Either[String, Year] = Try(Year(s)) match {
          case Success(year) => Right(year)
          case Failure(_)    => Left("That's not a year")
        }

        stringBinder.bind(key, params).map {
          case Right(s)      => withParameter(s)
          case Left(message) => Left(message)
        }
      }

      def unbind(key: String, value: Year): String = stringBinder.unbind(key, value.year)
    }

//  implicit def queryStringBindable: QueryStringBindable[Year] = new QueryStringBindable[Year] {
//
//    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Year]] = {
//      println("0" * 100)
//      println(params)
//      println(params.head._2)
//      println("0" * 100)
//      JsString(params.head._2.head).validate[Year] match {
//        case JsSuccess(year, _) =>
//          println("1" * 100)
//          println(year)
//          println("1" * 100)
//          Some(Right(year))
//        case _ =>
//          println("2" * 100)
//          Some(Left(s"Invalid year")) // TODO - Change
//      }
//    }
//
//    override def unbind(key: String, value: Year): String =
//      value.toString
//  }

//  implicit def pathBindable: PathBindable[Year] = new PathBindable[Year] {
//
//    override def bind(key: String, value: String): Either[String, Year] =
//      JsString(value).validate[Year] match {
//        case JsSuccess(year, _) =>
//          println("5" * 100)
//          println(year)
//          println("5" * 100)
//          Right(year)
//        case _ =>
//          println("6" * 100)
//          Left(s"Invalid year") // TODO - Change
//      }
//
//    override def unbind(key: String, value: Year): String =
//      value.toString
//  }
}
