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
import journey.PaymentToday
import play.api.Logger
import play.api.test.FakeRequest
import testsupport.ItSpec
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.LocalDate

class PaymentDatesServiceSpec extends ItSpec {
  private val logger = Logger(getClass)

  val clockProvider: ClockProvider = fakeApplication().injector.instanceOf[ClockProvider]
  val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]
  val paymentDatesService: PaymentDatesService = fakeApplication().injector.instanceOf[PaymentDatesService]

  val daysFromCreatedDateToProcessFirstPayment: Int = appConfig.daysToProcessUpfrontPayment
  val minGapBetweenPayments: Int = appConfig.minGapBetweenPayments

  def date(date: String): LocalDate = LocalDate.parse(date)

  "PaymentDatesService" - {
    ".paymentsCalendar" - {
      "returns a Payments Calendar for payments plans based on customer preferences for payment day and upfront payment " - {
        "no upfront payment and no payment day preference returns Payment Calendar" - {
          implicit val fakeRequest: FakeRequest[Any] = FakeRequest()
          val dateOfJourney = clockProvider.nowDate()
          val noPaymentToday = None
          val noArrangementDayOfMonth = None

          val result = paymentDatesService
            .paymentsCalendar(noPaymentToday, noArrangementDayOfMonth, dateOfJourney)(appConfig)

          "created on date of journey" in {
            result.planStartDate shouldBe dateOfJourney
          }
          "with no upfront payment date" in {
            result.maybeUpfrontPaymentDate shouldBe None
          }
          "with first regular payment date is at least " +
            s"${daysFromCreatedDateToProcessFirstPayment + minGapBetweenPayments} day/s from date of journey" in {
              result.regularPaymentDates.head.minusDays(daysFromCreatedDateToProcessFirstPayment + minGapBetweenPayments - 1)
                .isAfter(result.planStartDate)
            }
        }
        "upfront payment but no payment day preference returns Payment Calendar" - {
          implicit val fakeRequest: FakeRequest[Any] = FakeRequest()
          val dateOfJourney = clockProvider.nowDate()
          val paymentToday = Some(PaymentToday(true))
          val noArrangementDayOfMonth = None

          val result = paymentDatesService
            .paymentsCalendar(paymentToday, noArrangementDayOfMonth, dateOfJourney)(appConfig)
          "created on date of journey" in {
            result.planStartDate shouldBe dateOfJourney
          }
          s"with upfront payment date at least $daysFromCreatedDateToProcessFirstPayment day/s from date of journey" in {
            result
              .maybeUpfrontPaymentDate.get.minusDays(daysFromCreatedDateToProcessFirstPayment - 1)
              .isAfter(dateOfJourney) shouldBe true
          }
          s"with first regular payment date at least $minGapBetweenPayments day/s from upfront payment date" in {
            result.regularPaymentDates.head.minusDays(minGapBetweenPayments - 1)
              .isAfter(result.maybeUpfrontPaymentDate.get)
          }
        }
        "payment day preference but no upfront payment returns Payment Calendar" - {
          implicit val fakeRequest: FakeRequest[Any] = FakeRequest()
          val dateOfJourney = clockProvider.nowDate()
          val noPaymentToday = None
          val preferredRegularPaymentDay = Some(ArrangementDayOfMonth(1))

          val result = paymentDatesService
            .paymentsCalendar(noPaymentToday, preferredRegularPaymentDay, dateOfJourney)(appConfig)

          "created on date of journey" in {
            result.planStartDate shouldBe dateOfJourney
          }
          "with no upfront payment date" in {
            result.maybeUpfrontPaymentDate shouldBe None
          }
          s"with first regular payment date at least $daysFromCreatedDateToProcessFirstPayment day/s from date of journey" in {
            result.regularPaymentDates.head.minusDays(daysFromCreatedDateToProcessFirstPayment - 1)
              .isAfter(result.planStartDate)
          }
          "payment date for all regular payments is the customer's preferred day of payment" in {
            result.regularPaymentDates.foreach(_.getDayOfMonth shouldBe preferredRegularPaymentDay.get.dayOfMonth)
          }
        }
        "upfront payment and payment day preference returns Payment Calendar" - {
          implicit val fakeRequest: FakeRequest[Any] = FakeRequest()
          val dateOfJourney = clockProvider.nowDate()
          val paymentToday = Some(PaymentToday(true))
          val preferredRegularPaymentDay = Some(ArrangementDayOfMonth(1))

          val result = paymentDatesService
            .paymentsCalendar(paymentToday, preferredRegularPaymentDay, dateOfJourney)(appConfig)

          "created on date of journey" in {
            result.planStartDate shouldBe dateOfJourney
          }
          s"with upfront payment date at least $daysFromCreatedDateToProcessFirstPayment day/s from date of journey" in {
            result
              .maybeUpfrontPaymentDate.get.minusDays(daysFromCreatedDateToProcessFirstPayment - 1)
              .isAfter(dateOfJourney) shouldBe true
          }
          s"with first regular payment date at least $minGapBetweenPayments day/s from upfront payment date" in {
            result.regularPaymentDates.head.minusDays(minGapBetweenPayments - 1)
              .isAfter(result.maybeUpfrontPaymentDate.get)
          }
          "payment date for all regular payments is the customer's preferred day of payment" in {
            result.regularPaymentDates.foreach(_.getDayOfMonth shouldBe preferredRegularPaymentDay.get.dayOfMonth)
          }
        }

      }
    }
  }

}
