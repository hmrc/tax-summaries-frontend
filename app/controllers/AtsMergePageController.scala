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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.{AuthenticatedRequest, MergePageAuthAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AtsYearListService, AuditService, PayeAtsService}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.{AccountUtils, AttorneyUtils, Globals}
import view_models.AtsForms.atsYearFormMapping
import view_models.AtsList
import views.html.AtsMergePageView
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageController @Inject()(
  atsYearListService: AtsYearListService,
  val auditService: AuditService,
  payeAtsService: PayeAtsService,
  dataCacheConnector: DataCacheConnector,
  authAction: MergePageAuthAction,
  mcc: MessagesControllerComponents,
  atsMergePageView: AtsMergePageView,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView)(
  implicit formPartialRetriever: FormPartialRetriever,
  templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with AttorneyUtils with I18nSupport {

  def onPageLoad: Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[_] =>
    {

      for {
        saData   <- getSaYearList
        payeData <- getPayeAtsYearList
      } yield {
        (saData, payeData) match {
          case (Right(saTaxYearData), Right(payeTaxYearList)) =>
            Ok(
              atsMergePageView(
                saTaxYearData,
                payeTaxYearList,
                atsYearFormMapping,
                getActingAsAttorneyFor(request, saTaxYearData.forename, saTaxYearData.surname, saTaxYearData.utr)))
              .withSession(request.session + ("atsList" -> saTaxYearData.toString))
          case _ => InternalServerError(routes.ErrorController.serviceUnavailable().url)
        }
      }
    }
  }

  def getSaYearList()(implicit request: AuthenticatedRequest[_]): Future[Either[Int, AtsList]] = {
    if (request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER).equals(Some(Globals.TAXS_PORTAL_REFERENCE))) {

      val session = request.session + (Globals.TAXS_USER_TYPE_KEY -> Globals.TAXS_PORTAL_REFERENCE)
      val agentToken = request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID)

      agentToken.fold[Future[_]] {
        Future.successful(None)
      } { token =>
        if (AccountUtils.isAgent(request)) {
          dataCacheConnector.storeAgentToken(token) recover { case e: Throwable => throw e }
        } else {
          Future.successful(None)
        }
      }
    }
    atsYearListService.getAtsListData
  }

  def getPayeAtsYearList()(implicit request: AuthenticatedRequest[_]): Future[Either[HttpResponse, List[Int]]] = {

    val payeYear: Int = appConfig.payeYear

    request.nino.map(payeAtsService.getPayeTaxYearData(_, payeYear - 1, payeYear)).getOrElse(Future(Right(List.empty)))

  }

}
