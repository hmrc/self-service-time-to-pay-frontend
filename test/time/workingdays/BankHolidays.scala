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

package time.workingdays

import java.time.LocalDate

object BankHolidays {

  val eventSet =
    BankHolidaySet(
      "england-and-wales",
      List(
        BankHoliday(title = "New Year's Day", date = LocalDate.of(2012, 1, 2)),
        BankHoliday(title = "Easter Monday", date = LocalDate.of(2012, 4, 9)),
        BankHoliday(title = "Early May bank holiday", date = LocalDate.of(2012, 5, 7)),
        BankHoliday(title = "Spring bank holiday", date = LocalDate.of(2012, 6, 5)),
        BankHoliday(title = "Queen’s Diamond Jubilee", date = LocalDate.of(2012, 6, 5)),
        BankHoliday(title = "Summer bank holiday", date = LocalDate.of(2012, 8, 27)),
        BankHoliday(title = "Christmas Day", date = LocalDate.of(2012, 12, 25)),
        BankHoliday(title = "Boxing Day", date = LocalDate.of(2012, 12, 26)),
        BankHoliday(title = "New Year's Day", date = LocalDate.of(2013, 1, 1))
      )
    )
}
