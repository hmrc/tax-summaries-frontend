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

package controllers.auth

import com.google.inject.ImplementedBy
import controllers.auth.actions._
import controllers.auth.requests.{AuthenticatedRequest, PayeAuthenticatedRequest}
import play.api.mvc.{ActionBuilder, AnyContent}

import javax.inject.Inject

@ImplementedBy(classOf[AuthJourneyImpl])
trait AuthJourney {
  val authMinimal: ActionBuilder[AuthenticatedRequest, AnyContent]
  val authForIndividualsOrAgents: ActionBuilder[AuthenticatedRequest, AnyContent]
  val authForSAIndividualsOrAgents: ActionBuilder[AuthenticatedRequest, AnyContent]
  val authForPayeIndividuals: ActionBuilder[PayeAuthenticatedRequest, AnyContent]
}

class AuthJourneyImpl @Inject() (
  minAuthAction: MinAuthAction,
  authAction: AuthAction,
  payeAuthAction: PayeAuthAction
) extends AuthJourney {
  override val authMinimal: ActionBuilder[AuthenticatedRequest, AnyContent] = minAuthAction

  override val authForIndividualsOrAgents: ActionBuilder[AuthenticatedRequest, AnyContent] =
    authAction(saShutterCheck = false, agentTokenCheck = true, utrCheck = false)

  override val authForSAIndividualsOrAgents: ActionBuilder[AuthenticatedRequest, AnyContent] =
    authAction(saShutterCheck = true, agentTokenCheck = false, utrCheck = true)

  override val authForPayeIndividuals: ActionBuilder[PayeAuthenticatedRequest, AnyContent] = payeAuthAction
}
