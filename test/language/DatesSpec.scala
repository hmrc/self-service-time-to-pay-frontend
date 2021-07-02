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
import times.ClockProvider

import java.time.LocalDate

class DatesSpec extends ItSpec {
  import language.Dates._

  "getDayOfMonthOrdinal should show number with ordinal postfix" must {
    val d = LocalDate.parse("2000-01-01") // Mb: January has 31 days, so we can test all 31 ordinals with it

    "when language is English" in {
      implicit val englishMsgs = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

      getDayOfMonthOrdinal(d.withDayOfMonth(1)) mustBe "1st"
      getDayOfMonthOrdinal(d.withDayOfMonth(2)) mustBe "2nd"
      getDayOfMonthOrdinal(d.withDayOfMonth(3)) mustBe "3rd"
      getDayOfMonthOrdinal(d.withDayOfMonth(4)) mustBe "4th"
      getDayOfMonthOrdinal(d.withDayOfMonth(5)) mustBe "5th"
      getDayOfMonthOrdinal(d.withDayOfMonth(6)) mustBe "6th"
      getDayOfMonthOrdinal(d.withDayOfMonth(7)) mustBe "7th"
      getDayOfMonthOrdinal(d.withDayOfMonth(8)) mustBe "8th"
      getDayOfMonthOrdinal(d.withDayOfMonth(9)) mustBe "9th"
      getDayOfMonthOrdinal(d.withDayOfMonth(10)) mustBe "10th"
      getDayOfMonthOrdinal(d.withDayOfMonth(11)) mustBe "11th"
      getDayOfMonthOrdinal(d.withDayOfMonth(12)) mustBe "12th"
      getDayOfMonthOrdinal(d.withDayOfMonth(13)) mustBe "13th"
      getDayOfMonthOrdinal(d.withDayOfMonth(14)) mustBe "14th"
      getDayOfMonthOrdinal(d.withDayOfMonth(15)) mustBe "15th"
      getDayOfMonthOrdinal(d.withDayOfMonth(16)) mustBe "16th"
      getDayOfMonthOrdinal(d.withDayOfMonth(17)) mustBe "17th"
      getDayOfMonthOrdinal(d.withDayOfMonth(18)) mustBe "18th"
      getDayOfMonthOrdinal(d.withDayOfMonth(19)) mustBe "19th"
      getDayOfMonthOrdinal(d.withDayOfMonth(20)) mustBe "20th"
      getDayOfMonthOrdinal(d.withDayOfMonth(21)) mustBe "21st"
      getDayOfMonthOrdinal(d.withDayOfMonth(22)) mustBe "22nd"
      getDayOfMonthOrdinal(d.withDayOfMonth(23)) mustBe "23rd"
      getDayOfMonthOrdinal(d.withDayOfMonth(24)) mustBe "24th"
      getDayOfMonthOrdinal(d.withDayOfMonth(25)) mustBe "25th"
      getDayOfMonthOrdinal(d.withDayOfMonth(26)) mustBe "26th"
      getDayOfMonthOrdinal(d.withDayOfMonth(27)) mustBe "27th"
      getDayOfMonthOrdinal(d.withDayOfMonth(28)) mustBe "28th"
      getDayOfMonthOrdinal(d.withDayOfMonth(29)) mustBe "29th"
      getDayOfMonthOrdinal(d.withDayOfMonth(30)) mustBe "30th"
      getDayOfMonthOrdinal(d.withDayOfMonth(31)) mustBe "31st"
    }

    "when language is Welsh" in {
      implicit val welshMsgs = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))

      getDayOfMonthOrdinal(d.withDayOfMonth(1)) mustBe "1af"
      getDayOfMonthOrdinal(d.withDayOfMonth(2)) mustBe "2il"
      getDayOfMonthOrdinal(d.withDayOfMonth(3)) mustBe "3ydd"
      getDayOfMonthOrdinal(d.withDayOfMonth(4)) mustBe "4ydd"
      getDayOfMonthOrdinal(d.withDayOfMonth(5)) mustBe "5ed"
      getDayOfMonthOrdinal(d.withDayOfMonth(6)) mustBe "6ed"
      getDayOfMonthOrdinal(d.withDayOfMonth(7)) mustBe "7fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(8)) mustBe "8fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(9)) mustBe "9fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(10)) mustBe "10fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(11)) mustBe "11eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(12)) mustBe "12fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(13)) mustBe "13eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(14)) mustBe "14eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(15)) mustBe "15fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(16)) mustBe "16eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(17)) mustBe "17eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(18)) mustBe "18fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(19)) mustBe "19eg"
      getDayOfMonthOrdinal(d.withDayOfMonth(20)) mustBe "20fed"
      getDayOfMonthOrdinal(d.withDayOfMonth(21)) mustBe "21ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(22)) mustBe "22ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(23)) mustBe "23ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(24)) mustBe "24ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(25)) mustBe "25ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(26)) mustBe "26ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(27)) mustBe "27ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(28)) mustBe "28ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(29)) mustBe "29ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(30)) mustBe "30ain"
      getDayOfMonthOrdinal(d.withDayOfMonth(31)) mustBe "31ain"
    }
  }

  "getMonthFormatted should give long form human readable month as a word" should {
    val d = LocalDate.now()

    "when language is English" in {
      implicit val englishMsgs = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

      getMonthFormatted(d.withMonth(1)) mustBe "January"
      getMonthFormatted(d.withMonth(2)) mustBe "February"
      getMonthFormatted(d.withMonth(3)) mustBe "March"
      getMonthFormatted(d.withMonth(4)) mustBe "April"
      getMonthFormatted(d.withMonth(5)) mustBe "May"
      getMonthFormatted(d.withMonth(6)) mustBe "June"
      getMonthFormatted(d.withMonth(7)) mustBe "July"
      getMonthFormatted(d.withMonth(8)) mustBe "August"
      getMonthFormatted(d.withMonth(9)) mustBe "September"
      getMonthFormatted(d.withMonth(10)) mustBe "October"
      getMonthFormatted(d.withMonth(11)) mustBe "November"
      getMonthFormatted(d.withMonth(12)) mustBe "December"
    }

    "when language is Welsh" in {
      implicit val welshMsgs = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))

      getMonthFormatted(d.withMonth(1)) mustBe "Ionawr"
      getMonthFormatted(d.withMonth(2)) mustBe "Chwefror"
      getMonthFormatted(d.withMonth(3)) mustBe "Mawrth"
      getMonthFormatted(d.withMonth(4)) mustBe "Ebrill"
      getMonthFormatted(d.withMonth(5)) mustBe "Mai"
      getMonthFormatted(d.withMonth(6)) mustBe "Mehefin"
      getMonthFormatted(d.withMonth(7)) mustBe "Gorffennaf"
      getMonthFormatted(d.withMonth(8)) mustBe "Awst"
      getMonthFormatted(d.withMonth(9)) mustBe "Medi"
      getMonthFormatted(d.withMonth(10)) mustBe "Hydref"
      getMonthFormatted(d.withMonth(11)) mustBe "Tachwedd"
      getMonthFormatted(d.withMonth(12)) mustBe "Rhagfyr"
    }
  }

  "wholeDate should give formatted i18n'd day month and year like '5th November 2021'" should {
    "when language is English" in {
      implicit val english = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))
      wholeDate(LocalDate.parse("2021-11-05")) mustBe "5th November 2021"
    }

    "when language is Welsh" in {
      implicit val welsh = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))
      wholeDate(LocalDate.parse("2021-11-05")) mustBe "5ed Tachwedd 2021"
    }
  }
}
