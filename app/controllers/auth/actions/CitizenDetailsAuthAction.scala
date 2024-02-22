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

package controllers.auth.actions

import com.google.inject.{ImplementedBy, Inject}
import controllers.auth.requests.AuthenticatedRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}
import services.{CitizenDetailsService, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsAuthActionImpl @Inject() (
  cc: ControllerComponents,
  citizenDetailsService: CitizenDetailsService,
  override val authConnector: DefaultAuthConnector
) extends CitizenDetailsAuthAction
    with I18nSupport
    with Logging
    with AuthorisedFunctions {

  override def messagesApi: MessagesApi = cc.messagesApi

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    (request.nino, request.saUtr, request.agentRef) match {
      case (Some(nino), None, None) =>
        println("\na1:" + request.saUtr)
        getSAUTRFromCitizenDetails(nino).map {
          case retrievedSAUtr @ Some(_) => Right(request.copy(saUtr = retrievedSAUtr))
          case None                     =>
            println("\na2")
            Right(request)
        }
      case _          => Future.successful(Right(request))
    }
  }

  private def notAuthorisedPage: Result = Redirect(controllers.routes.ErrorController.notAuthorised)

  private def getSAUTRFromCitizenDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[SaUtr]] = {
    println("\nGETTING MATCHING DEETS:" + nino.nino)
    citizenDetailsService.getMatchingDetails(nino.nino).map {
      case SucccessMatchingDetailsResponse(matchingDetails) =>
        matchingDetails.saUtr match {
          case Some(_) =>
            println("\nRETURNED SAUTR")
            matchingDetails.saUtr
          case _       =>
            println("\nRETURNED NONE(2)")
            None
        }
      case _                                                =>
        println("\nRETURNED NONE")
        None
    }
  }

  override protected implicit val executionContext: ExecutionContext = cc.executionContext
}

@ImplementedBy(classOf[CitizenDetailsAuthActionImpl])
trait CitizenDetailsAuthAction extends ActionRefiner[AuthenticatedRequest, AuthenticatedRequest]
