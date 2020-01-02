/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

//TODO: rename it and merge it into ViewConfig.
class AppConfig @Inject() (servicesConfig: ServicesConfig) {

  private val contactFrontendBaseUrl = servicesConfig.getConfString("contact-frontend.url", "")
  private val contactFormServiceIdentifier = "self-service-time-to-pay"
  private lazy val feedbackSurveyFrontend = servicesConfig.getConfString("feedback-survey-frontend.url", "")

  lazy val assetsPrefix = servicesConfig.getString("assets.url") + servicesConfig.getString("assets.version")
  lazy val analyticsToken = servicesConfig.getString("google-analytics.token")
  lazy val analyticsHost = servicesConfig.getString("google-analytics.host")
  lazy val reportAProblemPartialUrl = s"$contactFrontendBaseUrl/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactFrontendBaseUrl/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrlNoAuth = s"$contactFrontendBaseUrl/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrlAuth = s"$contactFrontendBaseUrl/contact/beta-feedback?service=$contactFormServiceIdentifier"

  private lazy val companyAuthFrontend = servicesConfig.getConfString("company-auth.url", throw new RuntimeException("Company auth url required"))
  private lazy val companyAuthSignInPath = servicesConfig.getConfString("company-auth.sign-in-path", "")
  lazy val loginUrl: String = s"$companyAuthFrontend$companyAuthSignInPath"
  lazy val loginCallbackBaseUrl = servicesConfig.getConfString("auth.login-callback.base-url", "")
  lazy val loginCallbackPath = servicesConfig.getConfString("auth.login-callback.path", "")
  lazy val loginCallBackFullPath = s"$loginCallbackBaseUrl$loginCallbackPath"
  lazy val logoutUrl: String = s"$feedbackSurveyFrontend"

  // GA enhanced e-commerce custom vars
  lazy val initialPaymentMetric = servicesConfig.getConfString("google-analytics.initialPaymentMetric", "metric7")
  lazy val interestMetric = servicesConfig.getConfString("google-analytics.initialPaymentMetric", "metric8")
  lazy val durationMetric = servicesConfig.getConfString("google-analytics.initialPaymentMetric", "metric9")
  lazy val regularPaymentMetric = servicesConfig.getConfString("google-analytics.initialPaymentMetric", "metric10")
  lazy val monthlyDueDateMetric = servicesConfig.getConfString("google-analytics.monthlyDueDateMetric", "metric11")
  lazy val dueDateDimension = servicesConfig.getConfString("google-analytics.dueDateDimension", "dimension45")
  lazy val callForDirectDebitAssistanceDimension = servicesConfig.getConfString("google-analytics.dueDateDimension", "dimension46")
  lazy val printCompletePageDimension = servicesConfig.getConfString("google-analytics.dueDateDimension", "dimension47")
  lazy val clickFeedbackOnComplete = servicesConfig.getConfString("google-analytics.dueDateDimension", "dimension48")

}
