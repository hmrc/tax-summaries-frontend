package controllers.auth.requests

import uk.gov.hmrc.auth.core.retrieve.Credentials

trait CommonRequest {
  def isSa: Boolean
  def credentials: Credentials
}
