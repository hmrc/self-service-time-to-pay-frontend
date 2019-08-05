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

import org.joda.time.LocalDate
import org.scalatest.Inspectors
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.workingdays.BankHolidaySet

class BankHolidaysSpec extends UnitSpec with Inspectors {

  val hols: BankHolidaySet = BankHolidays()

  val tests: Seq[(String, String)] = List(
    ("New Year’s Day", "2020-01-01"),
    ("Good Friday", "2020-04-10"),
    ("Easter Monday", "2020-04-13"),
    ("Early May bank holiday", "2020-05-08"),
    ("Spring bank holiday", "2020-05-25"),
    ("Summer bank holiday", "2020-08-31"),
    ("Christmas Day", "2020-12-25"),
    ("Boxing Day", "2020-12-26"),
    //2021
    ("New Year’s Day", "2021-01-01"),
    ("Good Friday", "2021-04-02"),
    ("Easter Monday", "2021-04-05"),
    ("Early May bank holiday", "2021-05-03"),
    ("Spring bank holiday", "2021-05-31"),
    ("Summer bank holiday", "2021-08-30"),
    ("Christmas Day", "2021-12-25"),
    ("Boxing Day", "2021-12-26"),
    //2022
    ("New Year’s Day", "2022-01-01"),
    ("Good Friday", "2022-04-15"),
    ("Easter Monday", "2022-04-18"),
    ("Early May bank holiday", "2022-05-02"),
    ("Spring bank holiday", "2022-05-30"),
    ("Summer bank holiday", "2022-08-29"),
    ("Christmas Day", "2022-12-25"),
    ("Boxing Day", "2022-12-26"),
    //2023
    ("New Year’s Day", "2023-01-01"),
    ("Good Friday", "2023-04-07"),
    ("Easter Monday", "2023-04-10"),
    ("Early May bank holiday", "2023-05-01"),
    ("Spring bank holiday", "2023-05-29"),
    ("Summer bank holiday", "2023-08-28"),
    ("Christmas Day", "2023-12-25"),
    ("Boxing Day", "2023-12-26"),
    //2024
    ("New Year’s Day", "2024-01-01"),
    ("Good Friday", "2024-03-29"),
    ("Easter Monday", "2024-04-01"),
    ("Early May bank holiday", "2024-05-06"),
    ("Spring bank holiday", "2024-05-27"),
    ("Summer bank holiday", "2024-08-26"),
    ("Christmas Day", "2024-12-25"),
    ("Boxing Day", "2024-12-26"),
    //2025
    ("New Year’s Day", "2025-01-01"),
    ("Good Friday", "2025-04-18"),
    ("Easter Monday", "2025-04-21"),
    ("Early May bank holiday", "2025-05-05"),
    ("Spring bank holiday", "2025-05-26"),
    ("Summer bank holiday", "2025-08-25"),
    ("Christmas Day", "2025-12-25"),
    ("Boxing Day", "2025-12-26"),
    //2026
    ("New Year’s Day", "2026-01-01"),
    ("Good Friday", "2026-04-03"),
    ("Easter Monday", "2026-04-06"),
    ("Early May bank holiday", "2026-05-04"),
    ("Spring bank holiday", "2026-05-25"),
    ("Summer bank holiday", "2026-08-31"),
    ("Christmas Day", "2026-12-25"),
    ("Boxing Day", "2026-12-26"),
    //2027
    ("New Year’s Day", "2027-01-01"),
    ("Good Friday", "2027-03-26"),
    ("Easter Monday", "2027-03-29"),
    ("Early May bank holiday", "2027-05-03"),
    ("Spring bank holiday", "2027-05-31"),
    ("Summer bank holiday", "2027-08-30"),
    ("Christmas Day", "2027-12-25"),
    ("Boxing Day", "2027-12-26"),
    //2028
    ("New Year’s Day", "2028-01-01"),
    ("Good Friday", "2028-04-14"),
    ("Easter Monday", "2028-04-17"),
    ("Early May bank holiday", "2028-05-01"),
    ("Spring bank holiday", "2028-05-29"),
    ("Summer bank holiday", "2028-08-28"),
    ("Christmas Day", "2028-12-25"),
    ("Boxing Day", "2028-12-26"),
    //2029
    ("New Year’s Day", "2029-01-01"),
    ("Good Friday", "2029-03-30"),
    ("Easter Monday", "2029-04-02"),
    ("Early May bank holiday", "2029-05-07"),
    ("Spring bank holiday", "2029-05-28"),
    ("Summer bank holiday", "2029-08-27"),
    ("Christmas Day", "2029-12-25"),
    ("Boxing Day", "2029-12-26")

  )

  "BankHolidays" must {
    tests.foreach(row => {
      s"Check valid values - ${row._1} : ${row._2}" in {
        hols.events.filter(f => f.title == row._1 && f.date == LocalDate.parse(row._2)).size shouldBe 1
      }
    })
  }

}
