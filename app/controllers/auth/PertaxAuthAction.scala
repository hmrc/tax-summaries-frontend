package controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import connectors.PertaxConnector
import models.PertaxApiResponse
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.mvc.{ActionBuilder, ActionFunction, AnyContent, BodyParser, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Nino => AuthNino}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.errors.ServiceUnavailableView

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionImpl @Inject()(
                                       override val authConnector: DefaultAuthConnector,
                                       cc: ControllerComponents,
                                       pertaxConnector: PertaxConnector,
                                       serviceUnavailableView: ServiceUnavailableView
                                     )(implicit ec: ExecutionContext, appConfig: ApplicationConfig)
  extends PertaxAuthAction
    with I18nSupport
    with AuthorisedFunctions
    with Logging {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  override def invokeBlock[A](
                               request: Request[A],
                               block: PertaxAuthenticatedRequest[A] => Future[Result]
                             ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(AuthNino(hasNino = true)).retrieve(Retrievals.nino) {
      case Some(nino) =>
        pertaxConnector
          .pertaxAuth(nino)
          .transform {
            case Right(PertaxApiResponse("ACCESS_GRANTED", _, _, _)) => Right(block(PertaxAuthenticatedRequest(Nino(nino), request)))
            case Right(PertaxApiResponse("NO_HMRC_PT_ENROLMENT", _, _, Some(redirect))) =>
              Left(Future.successful(Redirect(s"$redirect?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}")))
            case Right(error) =>
              logger.error(s"Invalid code response from pertax with message: ${error.message}")
              Left(Future.successful(Redirect(controllers.paye.routes.PayeErrorController.notAuthorised)))
            case _ => Left(Future.successful(InternalServerError(serviceUnavailableView()(request, request2Messages(request), implicitly, implicitly))))
          }.merge.flatten
      case _ => throw new RuntimeException("Auth retrieval failed for user")
    }
  }

  override def messagesApi: MessagesApi = cc.messagesApi
}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PertaxAuthAction
  extends ActionBuilder[PertaxAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, PertaxAuthenticatedRequest]
