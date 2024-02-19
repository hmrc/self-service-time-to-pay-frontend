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

package language

import play.api.i18n.Messages

import java.time.LocalDate

object Dates {
  /**
   * @return the day of month will be formatted as an ordinal in english or welsh, e.g. "31st" or "31ain"
   */
  def getDayOfMonthOrdinal(localDate: LocalDate)
    (implicit messages: Messages): String = {
    val day = localDate.getDayOfMonth
    val ordinal = messages(s"date.ordinal.$day")
    s"$day$ordinal"
  }

  def getMonthFormatted(localDate: LocalDate)(implicit messages: Messages): String = {
    messages(s"date.${localDate.getMonth.toString.toLowerCase}")
  }

  /**
   * @return the formatted date in either english or welsh, such as "31st January 2021" or "31ain Ionawr 2021"
   */
  def wholeDate(localDate: LocalDate)
    (implicit messages: Messages): String = {
    val day = getDayOfMonthOrdinal(localDate)
    val month = getMonthFormatted(localDate)
    val year = localDate.getYear
    s"$day $month $year"
  }

  def monthYear(localDate: LocalDate)
    (implicit messages: Messages): String = {
    val month = getMonthFormatted(localDate)
    val year = localDate.getYear
    s"$month $year"
  }

}
