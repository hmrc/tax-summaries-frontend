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

package services

import com.google.inject.Inject
import connectors.MessageFrontendConnector
import models.MessageCount
import models.admin.SCAWrapperToggle
import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService

import scala.concurrent.{ExecutionContext, Future}

class MessageFrontendService @Inject() (
  messageFrontendConnector: MessageFrontendConnector,
  featureFlagService: FeatureFlagService
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def getUnreadMessageCount(implicit request: Request[_]): Future[Option[Int]] =
    featureFlagService.get(SCAWrapperToggle).flatMap { toggle =>
      if (toggle.isEnabled) {
        Future.successful(None)
      } else {
        messageFrontendConnector
          .getUnreadMessageCount()
          .fold(
            _ => None,
            response => response.json.asOpt[MessageCount].map(_.count)
          ) recover { case ex: Exception =>
          logger.error(ex.getMessage, ex)
          None
        }
      }
    }
}
