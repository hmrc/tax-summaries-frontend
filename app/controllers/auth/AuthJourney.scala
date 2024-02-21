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
  val authForIndividualsAndAgentsOnly: ActionBuilder[AuthenticatedRequest, AnyContent]
  // sa summaries agents + individs BUT ONLY SA else error page displayed:-
  val authForSAIndividualsAndAgentsOnly: ActionBuilder[AuthenticatedRequest, AnyContent]
  // paye pages individs only:-
  val authForPayeIndividualsOnly: ActionBuilder[PayeAuthenticatedRequest, AnyContent]
}

class AuthJourneyImpl @Inject() (
  selfAssessmentAction: SelfAssessmentAction,
  payeAuthAction: PayeAuthAction,
  pertaxPAYEAuthAction: PertaxPAYEAuthAction,
  pertaxSAAuthAction: PertaxSAAuthAction,
  minAuthAction: MinAuthAction,
  mergePageAuthAction: MergePageAuthAction
) extends AuthJourney {
  override val authMinimal: ActionBuilder[AuthenticatedRequest, AnyContent]                       =
    minAuthAction
  override val authForIndividualsAndAgentsOnly: ActionBuilder[AuthenticatedRequest, AnyContent]   =
    mergePageAuthAction
  override val authForSAIndividualsAndAgentsOnly: ActionBuilder[AuthenticatedRequest, AnyContent] =
    //authAction andThen selfAssessmentAction
    selfAssessmentAction andThen pertaxSAAuthAction
    
  /*
  SA:-
   active agent then so long as has basic creds let thru
   if not agent then do full frontend or backend auth depending on pertax auth toggle
   
  PAYE:-
  If paye shuttered then say service unavailable
  Do full frontend or backend auth depending on pertax auth toggle
  
  SO pattern to use:-
  
  SA: initial sa shuttered check then agent check then fe/ be auth depending on toggle then citizen details utr bit
  PAYE: initial paye shuttered check then fe/be auth depending on toggle
  
   */
  
  override val authForPayeIndividualsOnly: ActionBuilder[PayeAuthenticatedRequest, AnyContent]    =
    payeAuthAction andThen pertaxPAYEAuthAction
}
