/*
 * Copyright 2021 HM Revenue & Customs
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

package language

import play.api.i18n.{Lang, MessagesApi}
import testsupport.ItSpec

import java.time.LocalDate

class DatesSpec extends ItSpec {
  import language.Dates._

  "getDayOfMonthOrdinal should show number with ordinal postfix" - {
    val d = LocalDate.now()

    "when language is English" in {
      implicit val englishMsgs = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

      getDayOfMonthOrdinal(d.withDayOfMonth(1)) shouldBe "1st"
      getDayOfMonthOrdinal(d.withDayOfMonth(2)) shouldBe "2nd"
      getDayOfMonthOrdinal(d.withDayOfMonth(3)) shouldBe "3rd"
      getDayOfMonthOrdinal(d.withDayOfMonth(4)) shouldBe "4th"
      getDayOfMonthOrdinal(d.withDayOfMonth(5)) shouldBe "5th"
      getDayOfMonthOrdinal(d.withDayOfMonth(6)) shouldBe "6th"
      getDayOfMonthOrdinal(d.withDayOfMonth(7)) shouldBe "7th"
      getDayOfMonthOrdinal(d.withDayOfMonth(8)) shouldBe "8th"
      getDayOfMonthOrdinal(d.withDayOfMonth(9)) shouldBe "9th"
      getDayOfMonthOrdinal(d.withDayOfMonth(10)) shouldBe "10th"
      getDayOfMonthOrdinal(d.withDayOfMonth(11)) shouldBe "11th"
      getDayOfMonthOrdinal(d.withDayOfMonth(12)) shouldBe "12th"
      getDayOfMonthOrdinal(d.withDayOfMonth(13)) shouldBe "13th"
      getDayOfMonthOrdinal(d.withDayOfMonth(14)) shouldBe "14th"
      getDayOfMonthOrdinal(d.withDayOfMonth(15)) shouldBe "15th"
      getDayOfMonthOrdinal(d.withDayOfMonth(16)) shouldBe "16th"
      getDayOfMonthOrdinal(d.withDayOfMonth(17)) shouldBe "17th"
      getDayOfMonthOrdinal(d.withDayOfMonth(18)) shouldBe "18th"
      getDayOfMonthOrdinal(d.withDayOfMonth(19)) shouldBe "19th"
      getDayOfMonthOrdinal(d.withDayOfMonth(20)) shouldBe "20th"
      getDayOfMonthOrdinal(d.withDayOfMonth(21)) shouldBe "21st"
      getDayOfMonthOrdinal(d.withDayOfMonth(22)) shouldBe "22nd"
      getDayOfMonthOrdinal(d.withDayOfMonth(23)) shouldBe "23rd"
      getDayOfMonthOrdinal(d.withDayOfMonth(24)) shouldBe "24th"
      getDayOfMonthOrdinal(d.withDayOfMonth(25)) shouldBe "25th"
      getDayOfMonthOrdinal(d.withDayOfMonth(26)) shouldBe "26th"
      getDayOfMonthOrdinal(d.withDayOfMonth(27)) shouldBe "27th"
      getDayOfMonthOrdinal(d.withDayOfMonth(28)) shouldBe "28th"
      getDayOfMonthOrdinal(d.withDayOfMonth(29)) shouldBe "29th"
      getDayOfMonthOrdinal(d.withDayOfMonth(30)) shouldBe "30th"
      getDayOfMonthOrdinal(d.withDayOfMonth(31)) shouldBe "31st"
    }

    "when language is Welsh" in {
      implicit val welshMsgs = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))
      val d = LocalDate.now()

      getDayOfMonthOrdinal(d.withDayOfMonth(1)) shouldBe "1af"
      getDayOfMonthOrdinal(d.withDayOfMonth(2)) shouldBe "2il"
      getDayOfMonthOrdinal(d.withDayOfMonth(3)) shouldBe "3ydd"
      getDayOfMonthOrdinal(d.withDayOfMonth(4)) shouldBe "4ydd"
      getDayOfMonthOrdinal(d.withDayOfMonth(5)) shouldBe "5ed"
      getDayOfMonthOrdinal(d.withDayOfMonth(6)) shouldBe "6ed"
      getDayOfMonthOrdinal(d.withDayOfMonth(7)) shouldBe "7fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(8)) shouldBe "8fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(9)) shouldBe "9fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(10)) shouldBe "10fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(11)) shouldBe "11eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(12)) shouldBe "12fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(13)) shouldBe "13eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(14)) shouldBe "14eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(15)) shouldBe "15fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(16)) shouldBe "16eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(17)) shouldBe "17eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(18)) shouldBe "18fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(19)) shouldBe "19eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(20)) shouldBe "20fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(21)) shouldBe "21ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(22)) shouldBe "22ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(23)) shouldBe "23ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(24)) shouldBe "24ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(25)) shouldBe "25ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(26)) shouldBe "26ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(27)) shouldBe "27ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(28)) shouldBe "28ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(29)) shouldBe "29ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(30)) shouldBe "30ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(31)) shouldBe "31ain"
    }
  }

  "getMonthFormatted should give long form human readable month as a word" - {
    val d = LocalDate.now()

    "when language is English" in {
      implicit val englishMsgs = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

      getMonthFormatted(d.withMonth(1)) shouldBe "January"
      getMonthFormatted(d.withMonth(2)) shouldBe "February"
      getMonthFormatted(d.withMonth(3)) shouldBe "March"
      getMonthFormatted(d.withMonth(4)) shouldBe "April"
      getMonthFormatted(d.withMonth(5)) shouldBe "May"
      getMonthFormatted(d.withMonth(6)) shouldBe "June"
      getMonthFormatted(d.withMonth(7)) shouldBe "July"
      getMonthFormatted(d.withMonth(8)) shouldBe "August"
      getMonthFormatted(d.withMonth(9)) shouldBe "September"
      getMonthFormatted(d.withMonth(10)) shouldBe "October"
      getMonthFormatted(d.withMonth(11)) shouldBe "November"
      getMonthFormatted(d.withMonth(12)) shouldBe "December"
    }

    "when language is Welsh" in {
      implicit val welshMsgs = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))

      getMonthFormatted(d.withMonth(1)) shouldBe "Ionawr"
      getMonthFormatted(d.withMonth(2)) shouldBe "Chwefror"
      getMonthFormatted(d.withMonth(3)) shouldBe "Mawrth"
      getMonthFormatted(d.withMonth(4)) shouldBe "Ebrill"
      getMonthFormatted(d.withMonth(5)) shouldBe "Mai"
      getMonthFormatted(d.withMonth(6)) shouldBe "Mehefin"
      getMonthFormatted(d.withMonth(7)) shouldBe "Gorffennaf"
      getMonthFormatted(d.withMonth(8)) shouldBe "Awst"
      getMonthFormatted(d.withMonth(9)) shouldBe "Medi"
      getMonthFormatted(d.withMonth(10)) shouldBe "Hydref"
      getMonthFormatted(d.withMonth(11)) shouldBe "Tachwedd"
      getMonthFormatted(d.withMonth(12)) shouldBe "Rhagfyr"
    }
  }

  "wholeDate should give formatted i18n'd day month and year like '5th November 2021'" - {
    "when language is English" in {
      implicit val english = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))
      wholeDate(LocalDate.parse("2021-11-05")) shouldBe "5th November 2021"
    }

    "when language is Welsh" in {
      implicit val welsh = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))
      wholeDate(LocalDate.parse("2021-11-05")) shouldBe "5ed Tachwedd 2021"
    }
  }
}