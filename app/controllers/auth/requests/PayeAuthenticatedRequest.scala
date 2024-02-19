package controllers.auth.requests

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Nino

case class PayeAuthenticatedRequest[A](
  nino: Nino,
  isSa: Boolean,
  credentials: Credentials,
  request: Request[A]
) extends WrappedRequest[A](request)
    with CommonRequest
