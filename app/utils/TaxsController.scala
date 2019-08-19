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

package utils

import java.util.Date

import connectors.AuthenticationConnector
import controllers.routes
import models.{ErrorResponse, InvalidTaxYear}
import play.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Results.BadRequest
import play.api.mvc.{AnyContent, Request, Result}
import services._
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext => User}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import view_models.NoATSViewModel

import scala.concurrent.Future

abstract class TaxsController extends FrontendController
          with Actions
          with AccountUtils
          with AttorneyUtils
          with AuthenticationConnector {

  implicit val formPartialRetriever: FormPartialRetriever

  def auditService: AuditService

  type T <: GenericViewModel

  def obtainResult(data:T)(implicit user:User, request: Request[AnyRef]): Result

  def extractViewModel()(implicit user: User, request: Request[AnyRef]): Future[Either[ErrorResponse, GenericViewModel]]

  def show(implicit user: User, request: Request[AnyRef]): Future[Result] = {
    println("show req  --> " + request.toString)
    transformation recover {
      case error =>
        Logger.info(Globals.TAXS_LOGGER_ERROR_DESCR, error)
        error match {
          case token_error: AgentTokenException =>
            auditService.sendEvent(AuditTypes.Tx_FAILED, Map("userId" -> getAccountId(user), "error" -> token_error.message, "time" -> new Date().toString, "attemptedToken" -> request2flash.get(Globals.TAXS_AGENT_TOKEN_KEY).getOrElse("")))
            Ok(views.html.errors.token_error())
          case _ => Ok(views.html.errors.generic_error())
        }
    }
  }

  protected def transformation(implicit user: User, request: Request[AnyRef]): Future[Result] = {
    extractViewModel map {
      case Right(noAts: NoATSViewModel) => {
        println("redirect")
        Redirect(routes.ErrorController.authorisedNoAts())
      }
      case Right(result: T) => {
        println("result")
        obtainResult(result)
      }
      case Left(InvalidTaxYear) => {
        println("request")
        BadRequest("Request does not contain valid tax year")
      }

    }
  }

  def getParamAsInt(param: String, block: Int => Future[GenericViewModel])(implicit request: Request[AnyContent]) = {
    val intParam = request.body.asFormUrlEncoded.map(_(param).head.toInt).getOrElse(0)
    block(intParam)
  }

  protected def extractViewModel(genericViewModel: Int => Future[GenericViewModel])(implicit user: User, request: Request[AnyRef]):
  Future[Either[ErrorResponse, GenericViewModel]] = {
    println("%%%%%%% request -> " + request)
    TaxYearUtil.extractTaxYear match {
      case Right(taxYear) => {
        println("Here")
        genericViewModel(taxYear).map(Right(_))
      }
      case Left(errorResponse) => Future.successful(Left(errorResponse))
    }
  }
}
