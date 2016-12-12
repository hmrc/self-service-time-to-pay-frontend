/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.Logger
import play.api.mvc.{ActionBuilder, AnyContent, Controller, Request, Result, Action => PlayAction}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpDelete, HttpGet, HttpPut}
import uk.gov.hmrc.selfservicetimetopay.auth.{SaGovernmentGateway, SaRegime}
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig.ttpSessionId
import uk.gov.hmrc.selfservicetimetopay.connectors.{SessionCacheConnector => KeystoreConnector, _}
import uk.gov.hmrc.selfservicetimetopay.controllers._
import uk.gov.hmrc.selfservicetimetopay.models.TTPSubmission
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.util.{CheckSessionAction, SessionProvider}

import scala.concurrent.Future

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with AppName with RunMode with HttpAuditing {
  val auditConnector = FrontendAuditConnector
  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
}

object SessionCacheConnector extends KeystoreConnector with AppName with ServicesConfig {
  override val sessionKey: String = getConfString("keystore.sessionKey", throw new RuntimeException("Could not find session key"))
  override def defaultSource: String = appName
  override def baseUri: String = baseUrl("keystore")
  override def domain: String = getConfString("keystore.domain", throw new RuntimeException("Could not find config keystore.domain"))
  override def http: HttpGet with HttpPut with HttpDelete = WSHttp
}

object FrontendAuditConnector extends Auditing with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object FrontendAuthConnector extends AuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")
  override def http: HttpGet = WSHttp
}

object DirectDebitConnector extends DirectDebitConnector with ServicesConfig {
  lazy val directDebitURL: String = baseUrl("direct-debit")
  lazy val serviceURL = "direct-debit"
  lazy val http = WSHttp
}

object CalculatorConnector extends CalculatorConnector with ServicesConfig {
  val calculatorURL: String = baseUrl("time-to-pay-calculator")
  val serviceURL = "time-to-pay-calculator/paymentschedule"
  val http = WSHttp
}

object ArrangementConnector extends ArrangementConnector with ServicesConfig {
  val arrangementURL: String = baseUrl("time-to-pay-arrangement")
  val serviceURL = "ttparrangements"
  val http = WSHttp
}

object TaxPayerConnector extends TaxPayerConnector with ServicesConfig {
  val taxPayerURL: String = baseUrl("taxpayer")
  val serviceURL = "tax-payer"
  val http = WSHttp
}
object EligibilityConnector extends EligibilityConnector with ServicesConfig {
  val eligibilityURL: String = baseUrl("time-to-pay-eligibility")
  val serviceURL = "time-to-pay-eligibility/eligibility"
  val http = WSHttp
}

trait TimeToPayController extends FrontendController with Actions with CheckSessionAction {
  checkSessionAction: CheckSessionAction =>

  override val sessionProvider: SessionProvider = new SessionProvider() {}
  override lazy val authConnector: AuthConnector = FrontendAuthConnector
  protected lazy val sessionCache: KeystoreConnector = SessionCacheConnector
  protected lazy val Action: ActionBuilder[Request] = checkSessionAction andThen PlayAction
  protected type AsyncPlayUserRequest = AuthContext => Request[AnyContent] => Future[Result]
  protected lazy val authenticationProvider:GovernmentGateway = SaGovernmentGateway
  protected lazy val saRegime = SaRegime(authenticationProvider)
  private val timeToPayConfidenceLevel = new IdentityConfidencePredicate(ConfidenceLevel.L200, Future.successful(Redirect(routes.SelfServiceTimeToPayController.getTtpCallUs())))

  def AuthorisedSaUser(body: AsyncPlayUserRequest): PlayAction[AnyContent] = AuthorisedFor(saRegime, timeToPayConfidenceLevel).async(body)

  override implicit def hc(implicit request: Request[_]): HeaderCarrier = {
    request.cookies.find(_.name == ttpSessionId).fold(super.hc(request)) { id =>
      super.hc(request).withExtraHeaders(ttpSessionId -> id.value)
    }
  }

  protected def updateOrCreateInCache(found: (TTPSubmission) => TTPSubmission, notFound: () => TTPSubmission)
                                     (implicit hc: HeaderCarrier) = {
    sessionCache.get.flatMap {
      case Some(ttpSubmission) =>
        Logger.info("TTP data found - merging record")
        sessionCache.put(found(ttpSubmission))
      case None =>
        Logger.info("No TTP Submission data found in cache")
        sessionCache.put(notFound())
    }
  }
}

trait ServiceRegistry extends ServicesConfig {
  lazy val auditConnector: Auditing = FrontendAuditConnector
  lazy val directDebitConnector: DirectDebitConnector = DirectDebitConnector
  lazy val sessionCacheConnector: KeystoreConnector = SessionCacheConnector
  lazy val authConnector: AuthConnector = FrontendAuthConnector
  lazy val arrangementConnector: ArrangementConnector = ArrangementConnector
  lazy val eligibilityConnector: EligibilityConnector = EligibilityConnector
  lazy val calculatorConnector: CalculatorConnector = CalculatorConnector
  lazy val taxPayerConnector: TaxPayerConnector = TaxPayerConnector
}

trait ControllerRegistry { registry: ServiceRegistry =>
  private lazy val controllers = Map[Class[_], Controller](
    classOf[DirectDebitController] -> new DirectDebitController(directDebitConnector),
    classOf[CalculatorController] -> new CalculatorController(calculatorConnector),
    classOf[ArrangementController] -> new ArrangementController(directDebitConnector, arrangementConnector, calculatorConnector, taxPayerConnector, eligibilityConnector),
    classOf[EligibilityController] -> new EligibilityController(),
    classOf[SelfServiceTimeToPayController] -> new SelfServiceTimeToPayController()
  )

  def getController[A](controllerClass: Class[A]) : A = controllers(controllerClass).asInstanceOf[A]
}