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

package uk.gov.hmrc.selfservicetimetopay.controllers

import javax.inject.Inject
import play.api.Application
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.Call
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.language.LanguageController
import uk.gov.hmrc.selfservicetimetopay.config.DefaultRunModeAppNameConfig
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig._

class LanguageSwitchController @Inject() (override implicit val messagesApi: MessagesApi, implicit val app: Application)
  extends LanguageController with RunMode with DefaultRunModeAppNameConfig {

  def langToCall(lang: String): String => Call = routeToSwitchLanguage

  // Replace with a suitable fallback or read it from config
  override protected def fallbackURL: String = routes.SelfServiceTimeToPayController.start().url

  override def languageMap: Map[String, Lang] = languageMapValue
}
