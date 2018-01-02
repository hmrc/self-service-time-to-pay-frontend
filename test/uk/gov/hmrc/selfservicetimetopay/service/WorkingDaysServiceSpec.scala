/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.service

import java.time.LocalDate
import java.time.LocalDate.{of => d}
import org.joda.time.{LocalDate => JodaDate}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.Inspectors
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.selfservicetimetopay.connectors.BankHolidaysConnector
import uk.gov.hmrc.time.workingdays.{BankHoliday, BankHolidaySet}

import scala.concurrent.Future

class WorkingDaysServiceSpec extends UnitSpec  with MockitoSugar  with Inspectors {

  private case class Test(date: LocalDate, daysToAdd: Int, expected: LocalDate)

    val mockConnector = mock[BankHolidaysConnector]


  val fixedHolidaySet: BankHolidaySet =
    BankHolidaySet("england-and-wales", List(
      BankHoliday("some holiday", new JodaDate(2017, 3, 24)),
      //March 25,26 is weekend
      BankHoliday("some holiday", new JodaDate(2017, 3, 27))
    ))

   when(mockConnector.bankHolidays(any())(any())).thenReturn(Future.successful(fixedHolidaySet))

  "addWorkingDays" must {

    "skip over weekends as well as bank holidays" in {

      val tests = Seq[Test](
        Test(date = d(2017, 3, 22), daysToAdd = 1, expected = d(2017, 3, 23)),
        Test(date = d(2017, 3, 22), daysToAdd = 2, expected = d(2017, 3, 28)),
        Test(date = d(2017, 3, 23), daysToAdd = 1, expected = d(2017, 3, 28)),
        Test(date = d(2017, 3, 23), daysToAdd = 2, expected = d(2017, 3, 29))
      )

      forAll(tests) { test =>

          val service = new WorkingDaysService(mockConnector)
          service.addWorkingDays(test.date, test.daysToAdd) shouldBe test.expected
        }
    }
  }
}
