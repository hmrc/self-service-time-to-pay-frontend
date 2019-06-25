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

import com.google.inject.{AbstractModule, Provides, Singleton}
import play.api.Mode.Mode
import play.api.i18n._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.config.ServicesConfig

class Module extends AbstractModule {

  def configure(): Unit = ()

  @Provides
  @Singleton
  def serviceConfig(environment: Environment, configuration: Configuration): ServicesConfig = new ServicesConfig {
    def mode: Mode = environment.mode
    def runModeConfiguration: Configuration = configuration
  }

  @Provides
  @Singleton
  def authorisedFunctions(ac: AuthConnector): AuthorisedFunctions = new AuthorisedFunctions {
    override def authConnector: AuthConnector = ac
  }

  @Provides
  @Singleton
  def i18nSupport(environment: Environment, configuration: Configuration, langs: Langs): I18nSupport = new I18nSupport {
    override def messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, langs) {

      // return the key wrapped around a specific pattern so that automated tests can detect
      // stray message keys more easily
      override protected def noMatch(key: String, args: Seq[Any])(implicit lang: Lang): String = {
        Logger.error(s"Could not find message for key: $key")
        s"""message("$key")"""
      }
    }
  }
}
