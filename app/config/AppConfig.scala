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

import play.api.libs.json.{Json, OFormat}
import ssttpcalculator.CalculatorType
import ssttpcalculator.model.TaxLiability

import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

//TODO: merge it into ViewConfig.
class AppConfig @Inject() (servicesConfig: ServicesConfig) {
  private val contactFrontendBaseUrl = servicesConfig.getConfString("contact-frontend.url", "")
  private val contactFormServiceIdentifier = "self-service-time-to-pay"

  private val feedbackSurveyUrl =
    servicesConfig.getConfString(
      "feedback-survey.url",
      throw new RuntimeException("Feedback survey url required")) + "/feedback/PWYOII/personal"

  lazy val reportAProblemPartialUrl: String = s"$contactFrontendBaseUrl/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactFrontendBaseUrl/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val reportAccessibilityProblemUrl: String = s"$contactFrontendBaseUrl/contact/accessibility?service=$contactFormServiceIdentifier"

  private lazy val companyAuthFrontend = servicesConfig.getConfString("company-auth.url", throw new RuntimeException("Company auth url required"))
  private lazy val companyAuthSignInPath = servicesConfig.getConfString("company-auth.sign-in-path", "")

  lazy val loginUrl: String = s"$companyAuthFrontend$companyAuthSignInPath"
  lazy val loginCallbackBaseUrl: String = servicesConfig.getConfString("auth.login-callback.base-url", "")
  lazy val loginCallbackPath: String = servicesConfig.getConfString("auth.login-callback.path", "")
  lazy val loginCallBackFullPath = s"$loginCallbackBaseUrl$loginCallbackPath"
  lazy val logoutUrl: String = s"$feedbackSurveyUrl"
  lazy val backToTaxAccountUrl: String = servicesConfig.getString("back-to-tax-account.url")

  lazy val mdtpUpliftUrl: String = servicesConfig.getConfString("identity-verification-frontend.uplift-url",
    throw new RuntimeException("MDTP uplift url required"))

  lazy val (mdtpUpliftCompleteUrl, mdtpUpliftFailureUrl) = {
    val baseUrl = servicesConfig.getConfString("identity-verification-frontend.callback.base-url", "")

    val completePath = servicesConfig.getConfString("identity-verification-frontend.callback.complete-path",
      throw new RuntimeException("uplift continue path required"))

    val failurePath = servicesConfig.getConfString("identity-verification-frontend.callback.failure-path",
      throw new RuntimeException("uplift failure path required"))

    (s"$baseUrl$completePath", s"$baseUrl$failurePath")
  }

  lazy val minimumLengthOfPaymentPlan: Int = servicesConfig.getInt("paymentDatesConfig.minimumLengthOfPaymentPlan")
  lazy val maximumLengthOfPaymentPlan: Int = servicesConfig.getInt("paymentDatesConfig.maximumLengthOfPaymentPlan")
  lazy val daysToProcessFirstPayment: Int = servicesConfig.getInt("paymentDatesConfig.daysToProcessPayment")
  lazy val minGapBetweenPayments: Int = servicesConfig.getInt("paymentDatesConfig.minGapBetweenPayments")
  lazy val firstPaymentDayOfMonth: Int = servicesConfig.getInt("paymentDatesConfig.firstPaymentDayOfMonth")
  lazy val lastPaymentDayOfMonth: Int = servicesConfig.getInt("paymentDatesConfig.lastPaymentDayOfMonth")
  lazy val lastPaymentDelayDays: Int = servicesConfig.getInt("paymentDatesConfig.lastPaymentDelayDays")

  lazy val calculatorType: CalculatorType = servicesConfig.getString("calculatorType") match {
    case CalculatorType.Legacy.value           => CalculatorType.Legacy
    case CalculatorType.PaymentOptimised.value => CalculatorType.PaymentOptimised
    case otherValue                            => throw new Exception(s"calculator type '$otherValue' in config not recognised")
  }
}
