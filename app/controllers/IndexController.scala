/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.DataCacheConnector
import play.api.mvc.{Request, Result}
import services.{AtsListService, AtsYearListService, AuditService}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils._
import view_models.AtsForms._
import view_models.{NoATSViewModel, TaxYearEnd, AtsList}
import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object IndexController extends IndexController {
  override val atsYearListService = AtsYearListService
  override val auditService = AuditService
  override lazy val dataCache = DataCacheConnector
  override val atsListService = AtsListService
}

trait IndexController extends TaxsController {

  val dataCache: DataCacheConnector
  def atsYearListService: AtsYearListService
  def atsListService: AtsListService

  def authorisedIndex = AuthorisedFor(TaxSummariesRegime, GGConfidence).async {
    user => request => agentAwareShow(user, request)
  }

  def authorisedOnSubmit = AuthorisedFor(TaxSummariesRegime, GGConfidence).async {
    user => request => onSubmit(user, request)
  }

  //FIXME add extra check - agent tries multiple ids in same session
  def agentAwareShow(implicit user: User, request: Request[AnyRef]): Future[Result] =
    request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER) match {
      case Some(Globals.TAXS_PORTAL_REFERENCE) =>

        val session = request.session + (Globals.TAXS_USER_TYPE_KEY -> Globals.TAXS_PORTAL_REFERENCE)
        val agentToken = request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID)

        agentToken.fold[Future[_]] {
          Future.successful(None)
        } { token =>
          AccountUtils.isAgent(user) match {
            case true =>
              dataCache.storeAgentToken(token) recover { case e: Throwable => throw e }
            case _ =>
              Future.successful(None)
          }
        } map {
          x => Redirect(routes.IndexController.authorisedIndex()).withSession(session)
        }
      case _ => {
        show(user, request)
      }
    }

  type T = AtsList

  override def extractViewModel()(implicit user: User, request: Request[AnyRef]): Future[GenericViewModel] = {
    atsYearListService.getAtsListData
  }

  def getViewModel(result: T)(implicit user: User, request: Request[AnyRef]): Future[Result] = {
    result.yearList match {
      case TaxYearEnd(year) :: Nil => redirectWithYear(year.get.toInt)
      case _ => Future.successful(Ok(views.html.taxs_index(result, atsYearFormMapping, getActingAsAttorneyFor(user, result.forename, result.surname, result.utr))).withSession(request.session + ("atsList" -> result.toString)))
    }
  }

  override def transformation(implicit user: User, request: Request[AnyRef]): Future[Result] = {
    extractViewModel flatMap {
      case x: NoATSViewModel => Future { Redirect("") }
      case result: T => getViewModel(result)
    }
  }

  def onSubmit(implicit user: User, request: Request[AnyRef]): Future[Result] = {
    atsYearFormMapping.bindFromRequest.fold(
      formWithErrors => {
        val session = request.session + (Globals.TAXS_USER_TYPE_KEY -> Globals.TAXS_PORTAL_REFERENCE)
        atsListService.getAtsYearList flatMap {
          atsListData => {
            val atsList = new AtsList(atsListData.utr,
              atsListData.taxPayer.get.taxpayer_name.get("forename"),
              atsListData.taxPayer.get.taxpayer_name.get("surname"),
              atsListData.atsYearList.get.map(year => TaxYearEnd(Some(year.toString))))
            Future.successful(Ok(views.html.taxs_index(atsList, formWithErrors)).withSession(session))
          }
        }
      },
      value => redirectWithYear(value.year.get.toInt)
    )
  }

  def redirectWithYear(year: Int)(implicit user: User, request: Request[AnyRef]): Future[Result] = {
      atsListService.getAtsYearList flatMap {
        atsListData => {
          val taxYearListLength = atsListData.atsYearList.get.map(year => TaxYearEnd(Some(year.toString))).length
          Future(Redirect(routes.AtsMainController.authorisedAtsMain().url + "?taxYear=" + year).withSession(request.session + ("TaxYearListLength" -> taxYearListLength.toString)))
        }
      }
  }

  // This is unused, it is only implemented to adhere to the interface
  override def obtainResult(result: T)(implicit user: User, request: Request[AnyRef]): Result = {
    Ok(views.html.taxs_index(result, atsYearFormMapping, getActingAsAttorneyFor(user, result.forename, result.surname, result.utr)))
  }
}
