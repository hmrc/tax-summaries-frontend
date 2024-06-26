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
import config.ApplicationConfig
import controllers.auth.requests
import controllers.auth.requests.PayeAuthenticatedRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.PertaxAuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{Nino, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class PayeAuthActionImpl @Inject() (
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  pertaxAuthService: PertaxAuthService
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) extends PayeAuthAction
    with AuthorisedFunctions
    with Logging {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  private val payeShuttered: Boolean = appConfig.payeShuttered

  private def isAgent(enrolments: Set[Enrolment]) =
    enrolments
      .find(_.key == "IR-SA-AGENT")
      .flatMap { enrolment =>
        enrolment.identifiers
          .find(id => id.key == "IRAgentReference")
          .map(key => Uar(key.value))
      }
      .isDefined

  override def invokeBlock[A](
    request: Request[A],
    block: PayeAuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    if (payeShuttered) {
      Future.successful(Redirect(controllers.paye.routes.PayeErrorController.serviceUnavailable))
    } else {
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      pertaxAuthService.authorise[A, Request[A]](request).flatMap {
        case None    =>
          authorised(
            ConfidenceLevel.L200
          ).retrieve(Retrievals.allEnrolments and Retrievals.nino and Retrievals.credentials) {
            case Enrolments(enrolments) ~ Some(nino) ~ Some(credentials) =>
              if (isAgent(enrolments)) {
                Future.successful(Redirect(controllers.paye.routes.PayeErrorController.notAuthorised))
              } else {
                block {
                  requests.PayeAuthenticatedRequest(
                    nino = Nino(nino),
                    credentials = credentials,
                    request = request
                  )
                }
              }
            case _                                                       => throw new RuntimeException("Retrieval succeeded but did not match expectation")
          }
        case Some(r) => Future.successful(r)
      }
    }
}

@ImplementedBy(classOf[PayeAuthActionImpl])
trait PayeAuthAction
    extends ActionBuilder[PayeAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, PayeAuthenticatedRequest]
