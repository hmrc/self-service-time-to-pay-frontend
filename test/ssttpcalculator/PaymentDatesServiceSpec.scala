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

import play.api.Logger
import play.api.mvc.Request
import play.api.test.FakeRequest
import testsupport.ItSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC

import java.time.{Clock, LocalDate, LocalDateTime}

class PaymentDatesServiceSpec extends ItSpec {
  private val logger = Logger(getClass)

  val servicesConfig: ServicesConfig = fakeApplication().injector.instanceOf[ServicesConfig]
  val paymentDatesService: PaymentDatesService = fakeApplication().injector.instanceOf[PaymentDatesService]

  def date(date: String): LocalDate = LocalDate.parse(date)

  "PaymentDatesService" - {
    ".paymentsCalendar" - {
      "returns a Payments Calendar for payments plans based on customer's payment day and upfront payment preferences" - {
        "no upfront payment and no payment day preference returns Payment Calendar" - {
          val noPaymentToday = None
          val noArrangementDayOfMonth = None

          val result = paymentDatesService.paymentsCalendar(noPaymentToday, noArrangementDayOfMonth)(FakeRequest(), servicesConfig)
          "created on date of journey" in {
            result.createdOn shouldBe date("2019-11-25")

          }
          "with no upfront payment date" in {
            result.maybeUpfrontPaymentDate shouldBe None

          }
          "regular payments day 24 days from date of journey" in {
            val expectedGapFromCreatedOnToFirstPaymentDay = 24
            val dayDateCreatedPlusGap = result.createdOn.plusDays(expectedGapFromCreatedOnToFirstPaymentDay).getDayOfMonth
            result.regularPaymentsDay shouldEqual dayDateCreatedPlusGap
          }

        }
      }
    }
  }


}
