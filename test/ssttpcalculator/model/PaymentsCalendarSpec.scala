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

package ssttpcalculator.model

import play.api.Logger
import testsupport.ItSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate

class PaymentsCalendarSpec extends ItSpec {
  private val logger = Logger(getClass)

  implicit val servicesConfig: ServicesConfig = fakeApplication().injector.instanceOf[ServicesConfig]
  val daysFromCreatedDateToProcessFirstPayment: Int = servicesConfig.getInt("paymentDatesConfig.daysToProcessPayment")
  val minGapBetweenPayments: Int = servicesConfig.getInt("paymentDatesConfig.minGapBetweenPayments")
  val maximumLengthOfPaymentPlan: Int = servicesConfig.getInt("paymentDatesConfig.maximumLengthOfPaymentPlan")

  def date(date: String): LocalDate = LocalDate.parse(date)



  "PaymentsCalendar" - {
    ".regularPaymentDates" - {
      s"generates $maximumLengthOfPaymentPlan months' worth of dates" in {
        PaymentsCalendar(
          createdOn = date("2023-01-15"),
          maybeUpfrontPaymentDate = None,
          regularPaymentsDay = 28
        ).regularPaymentDates.length shouldBe maximumLengthOfPaymentPlan
      }
      "each on the regular payments day" in {
        val journeyPreferredPaymentDay = 28

        val paymentsCalendar = PaymentsCalendar(
          createdOn = date("2023-01-15"),
          maybeUpfrontPaymentDate = None,
          regularPaymentsDay = journeyPreferredPaymentDay
        )

        paymentsCalendar.regularPaymentDates.foreach(
          date => date.getDayOfMonth shouldBe journeyPreferredPaymentDay
        )
      }
      "starting on the first regular payment day" +
        s" at least $minGapBetweenPayments day/s after the upfront payment date" +
        ", when there is one" - {
        "works with regular payments day late in month" in {
          val journeyPreferredPaymentDay = 15
          val upfrontPaymentDate = date("2023-01-27")

          val firstRegularPaymentDate = PaymentsCalendar(
            createdOn = date("2023-01-15"),
            maybeUpfrontPaymentDate = Some(upfrontPaymentDate),
            regularPaymentsDay = journeyPreferredPaymentDay
          ).regularPaymentDates.head

          firstRegularPaymentDate.minusDays(minGapBetweenPayments - 1).isAfter(upfrontPaymentDate) shouldBe true
        }
        "works with regular payments day early in month" in {
          val journeyPreferredPaymentDay = 1
          val upfrontPaymentDate = date("2023-01-27")


          val firstRegularPaymentDate = PaymentsCalendar(
            createdOn = date("2023-01-15"),
            maybeUpfrontPaymentDate = Some(upfrontPaymentDate),
            regularPaymentsDay = journeyPreferredPaymentDay
          ).regularPaymentDates.head

          firstRegularPaymentDate.minusDays(minGapBetweenPayments - 1).isAfter(upfrontPaymentDate) shouldBe true
        }
      }
      "starting on the first regular payment day" +
        s" at least $daysFromCreatedDateToProcessFirstPayment day/s after calendar's date of creation" +
        ", if there is no upfront payment" - {
          "works with regular payments day late in month" in {
            val dateAtTimeOfJourney = date("2023-01-27")
            val journeyPreferredPaymentDay = 15

            val firstRegularPaymentDate = PaymentsCalendar(
              createdOn = dateAtTimeOfJourney,
              maybeUpfrontPaymentDate = None,
              regularPaymentsDay = journeyPreferredPaymentDay
            ).regularPaymentDates.head

            firstRegularPaymentDate
              .minusDays(daysFromCreatedDateToProcessFirstPayment - 1)
              .isAfter(dateAtTimeOfJourney) shouldBe true
          }
          "works with regular payments day early in month" in {
            val dateAtTimeOfJourney = date("2023-01-27")
            val journeyPreferredPaymentDay = 1


            val firstRegularPaymentDate = PaymentsCalendar(
              createdOn = dateAtTimeOfJourney,
              maybeUpfrontPaymentDate = None,
              regularPaymentsDay = journeyPreferredPaymentDay
            ).regularPaymentDates.head

            firstRegularPaymentDate
              .minusDays(daysFromCreatedDateToProcessFirstPayment - 1)
              .isAfter(dateAtTimeOfJourney) shouldBe true
          }
      }
    }
  }
}


