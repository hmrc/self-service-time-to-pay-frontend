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

package ssttpcalculator

import config.AppConfig
import org.scalatest.Assertion
import org.scalatest.time.{Seconds, Span}
import play.api.mvc.Results.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import ssttpcalculator.model.PaymentsCalendar
import testsupport.{ConfigSpec, RichMatchers}
import times.ClockProvider
import timetopaytaxpayer.cor.model.{CommunicationPreferences, SaUtr, SelfAssessmentDetails, Debit => corDebit}

class MaximumPlanLengthConfigSpec extends ConfigSpec{

  val testConfigMaxLengths: Seq[Int] = Seq(6, 12, 24)

  "PaymentsCalendar" - {
    "generates regular payment dates whose number is equal to the configurable maximum length of payment plan" - {
      testConfigMaxLengths.foreach { configuredMaxLength =>
        s"when maximum length of payment plan is set to $configuredMaxLength months" in {
          testPaymentsCalendarMaximumLength(configuredMaxLength)
        }
      }
    }
  }

  "PaymentPlansService.defaultSchedules" - {
    "generates plans up to the configurable maximum length but no more" - {
      testConfigMaxLengths.foreach { configuredMaxLength =>
        s"when maximum length of payment plan is set to $configuredMaxLength months" - {
          s"$configuredMaxLength month plan is generated" in {
            testDefaultSchedulesCanGenerateMaximumLength(configuredMaxLength)
          }
          s"${configuredMaxLength + 1} month plan is NOT generated" in {
            testDefaultSchedulesWillNotExceedMaximumLength(configuredMaxLength)
          }
        }
      }
    }
  }

  private def testPaymentsCalendarMaximumLength(configuredMaxLength: Int): Assertion = {
    val app = appWithConfigKeyValue("paymentDatesConfig.maximumLengthOfPaymentPlan", configuredMaxLength)
    val config: AppConfig = app.injector.instanceOf[AppConfig]

    val result = PaymentsCalendar.generate(0, date("2000-02-05"))(config)

    result.regularPaymentDates.length shouldEqual configuredMaxLength
  }

  private def testDefaultSchedulesCanGenerateMaximumLength(configuredMaxLength: Int): Assertion = {
    val app = appWithConfigKeyValue("paymentDatesConfig.maximumLengthOfPaymentPlan", configuredMaxLength)
    val paymentPlansService: PaymentPlansService = app.injector.instanceOf[PaymentPlansService]
    val request = FakeRequest()
    val nowDate = (new ClockProvider).nowDate()(request)

    val initialPayment = 0
    val preferredPaymentDay = None
    val remainingIncomeAfterSpending = 400

    val sa = SelfAssessmentDetails(
      SaUtr("saUtr"),
      CommunicationPreferences(
        welshLanguageIndicator = false,
        audioIndicator         = false,
        largePrintIndicator    = false,
        brailleIndicator       = false),
      Seq(corDebit("originCode",
        remainingIncomeAfterSpending * configuredMaxLength / 2,
        nowDate.plusMonths(configuredMaxLength + 3), None, nowDate
      )),
      Seq()
    )

    val result = paymentPlansService.defaultSchedules(
      sa,
      initialPayment,
      preferredPaymentDay,
      remainingIncomeAfterSpending
    )(request)
    result.toSeq.length shouldBe 3
  }

  private def testDefaultSchedulesWillNotExceedMaximumLength(configuredMaxLength: Int): Assertion = {
    val app = appWithConfigKeyValue("paymentDatesConfig.maximumLengthOfPaymentPlan", configuredMaxLength)
    val paymentPlansService: PaymentPlansService = app.injector.instanceOf[PaymentPlansService]
    val request = FakeRequest()
    val nowDate = (new ClockProvider).nowDate()(request)

    val initialPayment = 0
    val preferredPaymentDay = None
    val remainingIncomeAfterSpending = 400

    val sa = SelfAssessmentDetails(
      SaUtr("saUtr"),
      CommunicationPreferences(
        welshLanguageIndicator = false,
        audioIndicator         = false,
        largePrintIndicator    = false,
        brailleIndicator       = false
      ),
      Seq(corDebit("originCode",
        (remainingIncomeAfterSpending * configuredMaxLength / 2) + 1,
        nowDate.plusMonths(configuredMaxLength + 3),
        None,
        nowDate
      )),
      Seq()
    )

    val result = paymentPlansService.defaultSchedules(
      sa,
      initialPayment,
      preferredPaymentDay,
      remainingIncomeAfterSpending
    )(request)
    result.toSeq.length shouldBe 0
  }
}
