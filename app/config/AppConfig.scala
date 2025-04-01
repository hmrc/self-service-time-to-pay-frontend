/*
 * Copyright 2023 HM Revenue & Customs
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

package config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject

class AppConfig @Inject() (servicesConfig: ServicesConfig, config: Configuration) {

  private val feedbackSurveyUrl =
    servicesConfig.getConfString(
      "feedback-survey.url",
      throw new RuntimeException("Feedback survey url required")) + "/feedback/PWYOII/personal"

  private lazy val companyAuthFrontend = servicesConfig.getConfString("company-auth.url", throw new RuntimeException("Company auth url required"))
  private lazy val companyAuthSignInPath = servicesConfig.getConfString("company-auth.sign-in-path", "")

  lazy val loginUrl: String = s"$companyAuthFrontend$companyAuthSignInPath"
  lazy val logoutUrl: String = s"$feedbackSurveyUrl"

  private val signOutBaseUrl: String = BaseUrl.platformHost.getOrElse(config.get[String]("baseUrl.sign-out-local"))

  def signOutUrl(continueUrl: String): String =
    s"$signOutBaseUrl/bas-gateway/sign-out-without-state?continue=$continueUrl"

  lazy val backToTaxAccountUrl: String = servicesConfig.getString("urls.back-to-tax-account")
  lazy val webchatUrl: String = servicesConfig.getString("urls.webchat")

  lazy val maxLengthOfPaymentPlan: Int = servicesConfig.getInt("calculatorConfig.maximumLengthOfPaymentPlan")

  lazy val numberOfWorkingDaysToAdd = servicesConfig.getInt("calculatorConfig.firstPaymentTakenInNumberOfWorkingDays")

  lazy val payNowUrl = servicesConfig.getString("urls.pay-now")

  object BaseUrl {
    val platformHost: Option[String] = config.getOptional[String]("platform.frontend.host")
    val ssttpFrontend: String =
      platformHost.getOrElse(config.get[String]("baseUrl.ssttp-frontend-local"))
  }
}
