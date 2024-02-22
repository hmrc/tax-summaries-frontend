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
  // Merge page:-
  val authForIndividualsAndAgents: ActionBuilder[AuthenticatedRequest, AnyContent]
  // sa summaries agents + individs BUT ONLY SA else error page displayed:-
  val authForSAIndividualsAndAgentsOnly: ActionBuilder[AuthenticatedRequest, AnyContent]
  // paye pages individs only:-
  val authForPayeIndividualsOnly: ActionBuilder[PayeAuthenticatedRequest, AnyContent]
}

class AuthJourneyImpl @Inject() (
  minAuthAction: MinAuthAction,
  payeBasicAuthAction: PayeBasicAuthAction,
  saPertaxAuthAction: SaPertaxAuthAction,
  saCheckAuthAction: SaCheckAuthAction,
  payePertaxAuthAction: PayePertaxAuthAction,
  agentTokenAuthAction: AgentTokenAuthAction,
  citizenDetailsAuthAction: CitizenDetailsAuthAction
) extends AuthJourney {
  override val authMinimal: ActionBuilder[AuthenticatedRequest, AnyContent] =
    minAuthAction

  // Should not use saBasicAuthAction below: maybe MinAuthAction???
  // SO: next compare saBasicAuthAction and MinAuthAction: any differences apart from toggle check????
  //   ==> saBasicAuthAction same as MinAuthAction except for: sa toggle check, inactive agent check
  override val authForIndividualsAndAgents: ActionBuilder[AuthenticatedRequest, AnyContent]       =
    minAuthAction andThen agentTokenAuthAction andThen saPertaxAuthAction andThen citizenDetailsAuthAction
  override val authForSAIndividualsAndAgentsOnly: ActionBuilder[AuthenticatedRequest, AnyContent] =
    minAuthAction andThen saCheckAuthAction andThen saPertaxAuthAction andThen citizenDetailsAuthAction
  override val authForPayeIndividualsOnly: ActionBuilder[PayeAuthenticatedRequest, AnyContent]    =
    payeBasicAuthAction andThen payePertaxAuthAction
  /*
  SA:
   1) basic auth (sa shuttered check/ agent check + create request object with required fields) + iv uplift if etc toggle off then
   2) be auth if toggle then REFINER
   3) citizen details utr bit
  PAYE:
   1) basic auth (paye shuttered check + populate request object with required fields (get isSA from enrolments)) + iv uplift etc if toggle off then
   2) be auth depending on toggle REFINER

  MERGE PAGE:
   1) basic auth (same as sa)
   2) agent token auth
   3) be auth if toggle REFINER
   4) citizen details utr bit
   Same as SA basicAuthAction but also checks for agent token
   */
}
