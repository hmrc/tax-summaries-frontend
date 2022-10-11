package controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import connectors.PertaxConnector
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{ActionBuilder, ActionFunction, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import views.html.errors.ServiceUnavailableView

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionImpl @Inject()(
                                       override val authConnector: DefaultAuthConnector,
                                       cc: ControllerComponents,
                                       pertaxConnector: PertaxConnector,
                                       serviceUnavailableView: ServiceUnavailableView
                                     )(implicit ec: ExecutionContext, appConfig: ApplicationConfig)
  extends PayeAuthAction
    with I18nSupport
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](
                               request: Request[A],
                               block: PayeAuthenticatedRequest[A] => Future[Result]
                             ): Future[Result] = {



  }


}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PayeAuthAction
  extends ActionBuilder[PayeAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, PayeAuthenticatedRequest]
