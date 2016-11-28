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

import play.api.mvc.Controller
import uk.gov.hmrc.http.cache.client.{SessionCache => Keystore}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HttpDelete, HttpGet, HttpPut}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllers._

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with AppName with RunMode with HttpAuditing {
  val auditConnector = FrontendAuditConnector
  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
}

object SsttpSessionCache extends Keystore with AppName with ServicesConfig {
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
  lazy val directDebitURL = baseUrl("direct-debit")
  lazy val serviceURL = "direct-debit"
  lazy val http = WSHttp
}

object CalculatorConnector extends CalculatorConnector with ServicesConfig {
  val calculatorURL = baseUrl("self-service-time-to-pay")
  val serviceURL = "paymentschedule"
  val http = WSHttp
}

object ArrangementConnector extends ArrangementConnector with ServicesConfig {
  val arrangementURL = baseUrl("time-to-pay-arrangement")
  val serviceURL = "ttparrangements"
  val http = WSHttp
}

object TaxPayerConnector extends TaxPayerConnector with ServicesConfig {
  val taxPayerURL = baseUrl("time-to-pay-eligibility")
  val serviceURL = "time-to-pay-eligibility"
  val http = WSHttp
}
object EligibilityConnector extends EligibilityConnector with ServicesConfig {
  val eligibilityURL = baseUrl("time-to-pay-eligibility")
  val serviceURL = "eligibility"
  val http = WSHttp
}

trait ServiceRegistry extends ServicesConfig {
  lazy val auditConnector: Auditing = FrontendAuditConnector
  lazy val directDebitConnector: DirectDebitConnector = DirectDebitConnector
  lazy val sessionCache: Keystore = SsttpSessionCache
  lazy val authConnector: AuthConnector = FrontendAuthConnector
}

trait ControllerRegistry { registry: ServiceRegistry =>
  private lazy val controllers = Map[Class[_], Controller](
    classOf[DirectDebitController] -> new DirectDebitController(directDebitConnector, sessionCache, authConnector),
    classOf[ArrangementController] -> new ArrangementController(),
    classOf[CalculatorController] -> new CalculatorController(),
    classOf[EligibilityController] -> new EligibilityController(sessionCache),
    classOf[SelfServiceTimeToPayController] -> new SelfServiceTimeToPayController()
  )

  def getController[A](controllerClass: Class[A]) : A = controllers(controllerClass).asInstanceOf[A]
}