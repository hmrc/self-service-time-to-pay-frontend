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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Play
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.http.ws._

trait WSHttp extends HttpGet
  with WSGet
  with HttpPut
  with WSPut
  with HttpPost
  with WSPost
  with HttpPatch
  with WSPatch
  with HttpDelete
  with WSDelete
  with Hooks
  with AppName

object WSHttp extends WSHttp with DefaultRunModeAppNameConfig {
  protected def configuration: Option[Config] = None

  override protected def actorSystem: ActorSystem = Play.current.actorSystem
}

trait Hooks extends  HttpHooks with HttpAuditing {
  override lazy val auditConnector: FrontendAuditConnector.type = FrontendAuditConnector
  override val hooks: Seq[AuditingHook.type] = Seq(AuditingHook)
}

object FrontendAuditConnector extends Auditing with AppName with RunMode with DefaultRunModeAppNameConfig {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object FrontendAuthConnector extends AuthConnector with ServicesConfig with DefaultRunModeAppNameConfig {
  override val serviceUrl: String = baseUrl("auth")

  override def http: WSHttp.type = WSHttp
}
