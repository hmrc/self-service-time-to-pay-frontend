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
import play.api.mvc.{ActionBuilder, Controller, Request, Action => PlayAction}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpDelete, HttpGet, HttpPut}
import uk.gov.hmrc.selfservicetimetopay.connectors.{SessionCacheConnector => KeystoreConnector, _}
import uk.gov.hmrc.selfservicetimetopay.controllers._
import uk.gov.hmrc.selfservicetimetopay.models.TTPSubmission
import uk.gov.hmrc.selfservicetimetopay.controllers.calculator.{AmountsDueController, CalculateInstalmentsController, PaymentTodayController}
import uk.gov.hmrc.selfservicetimetopay.util.CheckSessionAction
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

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
  val calculatorURL: String = baseUrl("self-service-time-to-pay")
  val serviceURL = "paymentschedule"
  val http = WSHttp
}

object ArrangementConnector extends ArrangementConnector with ServicesConfig {
  val arrangementURL: String = baseUrl("time-to-pay-arrangement")
  val serviceURL = "ttparrangements"
  val http = WSHttp
}

object TaxPayerConnector extends TaxPayerConnector with ServicesConfig {
  val taxPayerURL: String = baseUrl("time-to-pay-eligibility")
  val serviceURL = "time-to-pay-eligibility"
  val http = WSHttp
}
object EligibilityConnector extends EligibilityConnector with ServicesConfig {
  val eligibilityURL: String = baseUrl("time-to-pay-eligibility")
  val serviceURL = "eligibility"
  val http = WSHttp
}

trait TimeToPayController extends FrontendController with Actions {
  override lazy val authConnector: AuthConnector = FrontendAuthConnector
  lazy val sessionCache: KeystoreConnector = SessionCacheConnector

  protected lazy val Action: ActionBuilder[Request] = CheckSessionAction andThen PlayAction

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
}

trait ControllerRegistry { registry: ServiceRegistry =>
  private lazy val controllers = Map[Class[_], Controller](
    classOf[DirectDebitController] -> new DirectDebitController(directDebitConnector),
    classOf[ArrangementController] -> new ArrangementController(directDebitConnector, arrangementConnector),
    classOf[CalculatorController] -> new CalculatorController(),
    classOf[EligibilityController] -> new EligibilityController(),
    classOf[SelfServiceTimeToPayController] -> new SelfServiceTimeToPayController(),
    classOf[AmountsDueController] -> new AmountsDueController(),
    classOf[CalculateInstalmentsController] -> new CalculateInstalmentsController(eligibilityConnector, calculatorConnector),
    classOf[PaymentTodayController] -> new PaymentTodayController()
  )

  def getController[A](controllerClass: Class[A]) : A = controllers(controllerClass).asInstanceOf[A]
}