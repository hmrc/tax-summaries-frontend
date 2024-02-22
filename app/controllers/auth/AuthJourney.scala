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
  saShutteredCheckAuthAction: SaShutteredCheckAuthAction,
  agentTokenAuthAction: AgentTokenAuthAction,
  citizenDetailsAuthAction: CitizenDetailsAuthAction,
  saUtrPresentAuthAction: SaUtrPresentAuthAction
) extends AuthJourney {
  override val authMinimal: ActionBuilder[AuthenticatedRequest, AnyContent] =
    minAuthAction

  // TODO: If saUtr is empty and not an agent then call citizen details to get utr. If can't find utr then just pass on request DONE
  // TODO: Merge page doesn't currently do an uplift in main - it should do (for non-agents).
  override val authForIndividualsAndAgents: ActionBuilder[AuthenticatedRequest, AnyContent] =
    minAuthAction andThen agentTokenAuthAction andThen citizenDetailsAuthAction andThen saPertaxAuthAction

  // TODO: 1) If backend auth toggle if OFF then do IV uplift.
  // TODO: 2) If saUtr is empty and not an agent then call citizen details to get utr. If can't find utr then not authorised. DONE
  override val authForSAIndividualsAndAgentsOnly: ActionBuilder[AuthenticatedRequest, AnyContent] =
    minAuthAction andThen saShutteredCheckAuthAction andThen citizenDetailsAuthAction andThen saPertaxAuthAction andThen saUtrPresentAuthAction

  // TODO: Merge payePertaxAuthAction into payeBasicAuthAction and then only do uplift etc if toggle off:-
  override val authForPayeIndividualsOnly: ActionBuilder[PayeAuthenticatedRequest, AnyContent] = payeBasicAuthAction
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
