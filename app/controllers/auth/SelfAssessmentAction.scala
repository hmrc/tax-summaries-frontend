/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.auth

import config.ApplicationConfig
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.CitizenDetailsService
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentAction @Inject()(
  citizenDetailsService: CitizenDetailsService,
  ninoAuthAction: NinoAuthAction,
  appConfig: ApplicationConfig)(implicit ec: ExecutionContext)
    extends ActionRefiner[AuthenticatedRequest, AuthenticatedRequest] {

  override protected def refine[A](
    request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {

    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    if (request.saUtr.isEmpty) {
      for {
        atsNinoResponse <- ninoAuthAction.getNino()
      } yield {
        atsNinoResponse match {
          case SuccessAtsNino(nino) =>
            val utr = citizenDetailsService.getUtr(nino)
            // val newRequest = request.copy(saUtr = Some(SaUtr("123123123")))
            Right(request)
          case NoAtsNinoFound =>
            Left(
              Redirect(controllers.routes.ErrorController.notAuthorised())
            )
          case UpliftRequiredAtsNino =>
            Left(
              Redirect(
                appConfig.identityVerificationUpliftUrl,
                Map(
                  "origin"          -> Seq(appConfig.appName),
                  "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
                  "completionURL"   -> Seq(appConfig.payeLoginCallbackUrl),
                  "failureURL"      -> Seq(appConfig.iVUpliftFailureCallback)
                )
              )
            )
        }
      }
    } else {
      Future(Right(request))
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
