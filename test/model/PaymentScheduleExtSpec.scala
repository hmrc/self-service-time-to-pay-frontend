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

package model

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpec}
import testsupport.testdata.TdAll._
import timetopaycalculator.cor.model.{Instalment, PaymentSchedule}

class PaymentScheduleExtSpec extends WordSpec with Matchers {
  private val calculatorPaymentSchedule = new PaymentSchedule(
    startDate            = LocalDate.of(2019: Int, 4: Int, 23: Int),
    endDate              = LocalDate.of(2019: Int, 8: Int, 21: Int),
    initialPayment       = 123: BigDecimal,
    amountToPay          = 2500: BigDecimal,
    instalmentBalance    = 200: BigDecimal,
    totalInterestCharged = 200: BigDecimal,
    totalPayable         = 200: BigDecimal,
    instalments          =
      List(
        new Instalment("2019-08-25", 300: BigDecimal, 2: BigDecimal),
        new Instalment("2019-08-25", 120: BigDecimal, 4: BigDecimal),
        new Instalment("2019-08-25", 250: BigDecimal, 3: BigDecimal)))

  private val firstInstalment = new Instalment("2019-08-25", 300: BigDecimal, 2: BigDecimal)
  private val middleInstalment = firstInstalment.copy(paymentDate = firstInstalment.paymentDate.plusMonths(1))
  private val lastInstalment = firstInstalment.copy(paymentDate = firstInstalment.paymentDate.plusMonths(2))

  private val scheduleWithoutInstallments = calculatorPaymentSchedule.copy(instalments = Seq.empty)
  private val scheduleWithASingleInstallment = calculatorPaymentSchedule.copy(instalments = Seq(firstInstalment))
  private val scheduleWithInstallmentsWithDifferentPaymentDates =
    calculatorPaymentSchedule.copy(instalments = Seq(firstInstalment, middleInstalment, lastInstalment))
  private val scheduleWithInstallmentsWithIdenticalPaymentDates =
    calculatorPaymentSchedule.copy(instalments = Seq(firstInstalment, firstInstalment, firstInstalment))

  "firstInstallment" should {
    "return the installment with the earliest payment date" when {
      "there are multiple installments with different payment dates" in {
        scheduleWithInstallmentsWithDifferentPaymentDates.firstInstallment shouldBe firstInstalment
      }
    }

    "return an installment" when {
      "there are multiple installments with the same payment date" in {
        scheduleWithInstallmentsWithIdenticalPaymentDates.firstInstallment.paymentDate shouldBe firstInstalment.paymentDate
      }
    }

    "return the installment" when {
      "there is a single installment" in {
        scheduleWithASingleInstallment.firstInstallment shouldBe firstInstalment
      }
    }

    "throw an exception" when {
      "there are no installments" in {
        intercept[RuntimeException] {
          scheduleWithoutInstallments.firstInstallment
        }
      }
    }
  }

  "lastInstallment" should {
    "return the installment with the latest payment date" when {
      "there are multiple installments with different payment dates" in {
        scheduleWithInstallmentsWithDifferentPaymentDates.lastInstallment shouldBe lastInstalment
      }
    }

    "return an installment" when {
      "there are multiple installments with the same payment date" in {
        scheduleWithInstallmentsWithIdenticalPaymentDates.lastInstallment.paymentDate shouldBe firstInstalment.paymentDate
      }
    }

    "return the installment" when {
      "there is a single installment" in {
        scheduleWithASingleInstallment.lastInstallment shouldBe firstInstalment
      }
    }

    "throw an exception" when {
      "there are no installments" in {
        intercept[RuntimeException] {
          scheduleWithoutInstallments.lastInstallment
        }
      }
    }
  }
}
