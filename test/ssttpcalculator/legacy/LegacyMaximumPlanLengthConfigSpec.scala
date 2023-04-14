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

package ssttpcalculator.legacy

import config.AppConfig
import org.scalatest.Assertion
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.test.FakeRequest
import ssttpcalculator.model.PaymentsCalendar
import testsupport.{ConfigSpec, ItSpec}
import times.ClockProvider
import timetopaytaxpayer.cor.model.{CommunicationPreferences, SaUtr, SelfAssessmentDetails, Debit => corDebit}

import java.time.LocalDate

class LegacyMaximumPlanLengthConfigSpec extends ConfigSpec {

  val testConfigMaxLengths: Seq[Int] = Seq(6, 12, 24)

  "CalculatorService.defaultSchedules" - {
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

  private def testDefaultSchedulesCanGenerateMaximumLength(configuredMaxLength: Int): Assertion = {
    val calculatorService: CalculatorService = appWithConfig(
      Map("legacyCalculatorConfig.maximumLengthOfPaymentPlan" -> configuredMaxLength)
    ).injector.instanceOf[CalculatorService]

    val request = FakeRequest()

    val clock = new ClockProvider
    val nowDate = clock.nowDate()(request)

    val initialPayment = 0
    val preferredPaymentDay = None
    val remainingIncomeAfterSpending = 400

    val sa = SelfAssessmentDetails(
      SaUtr("saUtr"),
      CommunicationPreferences(false, false, false, false),
      Seq(corDebit("originCode",
        remainingIncomeAfterSpending * configuredMaxLength / 2,
        nowDate.plusMonths(configuredMaxLength + 3), None, nowDate
      )),
      Seq()
    )

    val allSchedules = calculatorService.allAvailableSchedules(sa, initialPayment, preferredPaymentDay)(request)
    val closestSchedule = calculatorService.closestSchedule(remainingIncomeAfterSpending / 2, allSchedules)

    val result = calculatorService.defaultSchedules(closestSchedule, allSchedules)
    result.toSeq.length shouldBe 3
  }

  private def testDefaultSchedulesWillNotExceedMaximumLength(configuredMaxLength: Int): Assertion = {
    val calculatorService: CalculatorService = appWithConfig(
      Map("legacyCalculatorConfig.maximumLengthOfPaymentPlan" -> configuredMaxLength)
    ).injector.instanceOf[CalculatorService]

    val request = FakeRequest()

    val clock = new ClockProvider
    val nowDate = clock.nowDate()(request)

    val initialPayment = 0
    val preferredPaymentDay = None
    val remainingIncomeAfterSpending = 400

    val sa = SelfAssessmentDetails(
      SaUtr("saUtr"),
      CommunicationPreferences(false, false, false, false),
      Seq(corDebit("originCode",
        (remainingIncomeAfterSpending * configuredMaxLength / 2) + 1,
        nowDate.plusMonths(configuredMaxLength + 3),
        None,
        nowDate
      )),
      Seq()
    )

    val allSchedules = calculatorService.allAvailableSchedules(sa, initialPayment, preferredPaymentDay)(request)

    val closestSchedule = calculatorService.closestSchedule(remainingIncomeAfterSpending / 2, allSchedules)

    val result = calculatorService.defaultSchedules(closestSchedule, allSchedules)
    result.toSeq.length shouldBe 0
  }

}
