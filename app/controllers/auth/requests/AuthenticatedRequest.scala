package controllers.auth.requests

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.{Nino, SaUtr, Uar}

case class AuthenticatedRequest[A](
  userId: String,
  agentRef: Option[Uar],
  saUtr: Option[SaUtr],
  nino: Option[Nino],
  isSa: Boolean,
  isAgentActive: Boolean,
  confidenceLevel: ConfidenceLevel,
  credentials: Credentials,
  request: Request[A]
) extends WrappedRequest[A](request)
    with CommonRequest
