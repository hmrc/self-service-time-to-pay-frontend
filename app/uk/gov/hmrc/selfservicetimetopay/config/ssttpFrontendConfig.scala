/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val betaFeedbackUrlNoAuth :String
  val betaFeedbackUrlAuth :String

}

object SsttpFrontendConfig extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactFrontendBaseUrl = baseUrl("contact-frontend")
  private val contactFormServiceIdentifier = "self-service-time-to-pay"
  private val feedbackSurveyFrontend = baseUrl("feedback-survey-frontend")

  override lazy val assetsPrefix = loadConfig("assets.url") + loadConfig("assets.version")
  override lazy val analyticsToken = loadConfig("google-analytics.token")
  override lazy val analyticsHost = loadConfig("google-analytics.host")
  override lazy val reportAProblemPartialUrl = s"$contactFrontendBaseUrl/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactFrontendBaseUrl/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUrlNoAuth = s"$contactFrontendBaseUrl/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUrlAuth = s"$contactFrontendBaseUrl/contact/beta-feedback?service=$contactFormServiceIdentifier"

  private lazy val companyAuthFrontend = getConfString("company-auth.url", throw new RuntimeException("Company auth url required"))
  private lazy val companyAuthSignInPath = getConfString("company-auth.sign-in-path", "")
  private lazy val companyAuthSignOutPath = getConfString("company-auth.sign-out-path", "")
  lazy val loginUrl: String = s"$companyAuthFrontend$companyAuthSignInPath"
  lazy val loginCallbackBaseUrl = getConfString("auth.login-callback.base-url", "")
  lazy val loginCallbackPath = getConfString("auth.login-callback.path", "")
  lazy val loginCallBackFullPath = s"$loginCallbackBaseUrl$loginCallbackPath"
  lazy val logoutUrl: String = s"$feedbackSurveyFrontend/feedback-survey/?origin=PWYOII"


  // GA enhanced e-commerce custom vars
  lazy val initialPaymentMetric = getConfString("google-analytics.initialPaymentMetric", "metric7")
  lazy val interestMetric = getConfString("google-analytics.initialPaymentMetric", "metric8")
  lazy val durationMetric = getConfString("google-analytics.initialPaymentMetric", "metric9")
  lazy val regularPaymentMetric = getConfString("google-analytics.initialPaymentMetric", "metric10")
  lazy val monthlyDueDateMetric = getConfString("google-analytics.monthlyDueDateMetric", "metric11")
  lazy val dueDateDimension = getConfString("google-analytics.dueDateDimension", "dimension45")
  lazy val callForDirectDebitAssistanceDimension = getConfString("google-analytics.dueDateDimension", "dimension46")
  lazy val printCompletePageDimension = getConfString("google-analytics.dueDateDimension", "dimension47")
  lazy val clickFeedbackOnComplete = getConfString("google-analytics.dueDateDimension", "dimension48")
}
