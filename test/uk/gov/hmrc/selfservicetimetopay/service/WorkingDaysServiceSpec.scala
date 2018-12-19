/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.Inspectors
import uk.gov.hmrc.play.test.UnitSpec

class WorkingDaysServiceSpec extends UnitSpec   with Inspectors {

  private case class Test(date: LocalDate, daysToAdd: Int, expected: LocalDate)

  "addWorkingDays" must {

    "skip over weekends as well as bank holidays" in {

      val tests = Seq[Test](
        Test(date = d(2019, 12, 24), daysToAdd = 1, expected = d(2019, 12, 27)),
        Test(date = d(2018, 10, 5), daysToAdd = 1, expected = d(2018, 10, 8)),
        Test(date = d(2018, 10, 5), daysToAdd = 5, expected = d(2018, 10, 12)),
        Test(date = d(2018, 10, 5), daysToAdd = 6, expected = d(2018, 10, 15))
      )

      forAll(tests) { test =>

          val service = new WorkingDaysService()
          service.addWorkingDays(test.date, test.daysToAdd) shouldBe test.expected
        }
    }
  }
}
