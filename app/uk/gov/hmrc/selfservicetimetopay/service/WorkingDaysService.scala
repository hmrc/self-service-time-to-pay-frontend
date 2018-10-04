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

import javax.inject.{Inject, Singleton}
import org.joda.time
import play.api.libs.json.OFormat
import uk.gov.hmrc.time.workingdays._

import scala.language.postfixOps

@Singleton
class WorkingDaysService @Inject()() {

  //todo perhaps read from file
  //todo figure out why connector was not working in Prod?
  //todo quick workaround to fix prod
  implicit val hols: BankHolidaySet = BankHolidays()

  def addWorkingDays(date: LocalDate, days: Int): LocalDate = {
    val joda = org.joda.time.LocalDate.now().withMonthOfYear(date.getMonthValue).withDayOfMonth(date.getDayOfMonth).withYear(date.getYear)
    val jodaLocalDate: time.LocalDate = joda.plusWorkingDays(days)
    LocalDate.now().withYear(jodaLocalDate.getYear).withDayOfMonth(jodaLocalDate.getDayOfMonth).withMonth(jodaLocalDate.getMonthOfYear)
  }
}

import play.api.libs.json.Json

import scala.io.Source
//todo move this else where
object ResourceReader {
  def read(resourcePath: String): String = Source
    .fromInputStream(
      this.getClass.getResourceAsStream(resourcePath)
    )
    .getLines()
    .mkString("\n")
}


object BankHolidays {
  lazy implicit val bankHolidayFmt: OFormat[BankHolidaySet] = Json.format[BankHolidaySet]
  lazy implicit val BankHolidays: OFormat[BankHoliday] = Json.format[BankHoliday]
  type BankHolidaysByRegion = Map[String, BankHolidaySet]

  def apply(): BankHolidaySet = {

    val jsonString = ResourceReader.read("/bank_holidays.json")

    val json = Json.parse(jsonString)
    val map = json.as[BankHolidaysByRegion]
    map.apply("england-and-wales")
  }
}