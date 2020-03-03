/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.i18n.Messages
import req.RequestSupport

object Dates {

  val `format d MMMM y` = DateTimeFormatter.ofPattern("d MMMM y")

  def formatDate(date: LocalDate): String = `format d MMMM y`.format(date)

  def getMonthlyDateFormatted(localDate: LocalDate)(implicit messages: Messages): String = RequestSupport.language(messages) match {
    //TODO: Welsh
    case _ =>
      val date = localDate.getDayOfMonth.toString
      val postfix = {
        if (date == "11" || date == "12" || date == "13") "th"
        else if (date.endsWith("1")) "st"
        else if (date.endsWith("2")) "nd"
        else if (date.endsWith("3")) "rd"
        else "th"
      }
      s"$date$postfix"
  }

  def wholeDate(localDate: LocalDate)(implicit messages: Messages): String = {
    val day = getMonthlyDateFormatted(localDate)
    val month = localDate.getMonth.toString.toLowerCase.capitalize
    val year = localDate.getYear
    s"$day $month $year"
  }

}
