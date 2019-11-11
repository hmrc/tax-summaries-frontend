package controllers.auth

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.domain.Nino

case class AuthenticatedRequest[A](nino: Nino, request: Request[A]) extends WrappedRequest[A](request)
