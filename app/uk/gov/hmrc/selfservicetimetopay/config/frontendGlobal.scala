/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.config

import akka.stream.Materializer
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Mode.Mode
import play.api.i18n._
import play.api.mvc._
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters._
import uk.gov.hmrc.selfservicetimetopay.testonly.routes

import scala.concurrent.Future

object FrontendGlobal extends DefaultFrontendGlobal with MicroserviceFilterSupport with I18nSupport {

  implicit lazy val messagesApi: MessagesApi = Play.current.injector.instanceOf[MessagesApi]
  implicit lazy val auditConnector = FrontendAuditConnector

  override val loggingFilter = LoggingFilter
  override val frontendAuditFilter = AuditFilter

  override def frontendFilters: Seq[EssentialFilter] = {

    //We want to replace SessionTimeoutFilter with SessionTimeoutFilterWrapper
    //which omits application of this filter for 'TestUsersController.logIn()' functionality.
    val t = defaultFrontendFilters.map {
      case s: SessionTimeoutFilter => new SessionTimeoutFilterWrapper(s)
      case x                       => x
    }

    t ++ Seq(NoCacheFilter)
  }

  override def onStart(app: Application) {
    super.onStart(app)
    new ApplicationCrypto(Play.current.configuration.underlying).verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html = {

    views.html.selfservicetimetopay.error_template(pageTitle, heading, message)
  }

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig("microservice.metrics")

}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object AuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {

  override lazy val maskedFormFields: Seq[String] = Seq("password")

  override lazy val applicationPort: Option[Int] = None

  override lazy val auditConnector: FrontendAuditConnector.type = FrontendAuditConnector

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration

  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

/**
 * Special case for SessionTimeoutFilter which doesn't apply for one test-only endpoint.
 */
class SessionTimeoutFilterWrapper(sessionTimeoutFilter: SessionTimeoutFilter) extends Filter {
  override implicit def mat: Materializer = sessionTimeoutFilter.mat
  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (isSpecialRequest(rh)) f(rh) else sessionTimeoutFilter(f)(rh)

  private def isSpecialRequest(rh: RequestHeader) =
    rh.method == routes.TestUsersController.logIn().method &&
      rh.path == routes.TestUsersController.logIn().path()
}
