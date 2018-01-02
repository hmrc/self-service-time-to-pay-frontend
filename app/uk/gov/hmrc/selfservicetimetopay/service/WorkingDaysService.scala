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
import javax.inject.Inject

import org.joda.time
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors.BankHolidaysConnector
import uk.gov.hmrc.time.workingdays._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class WorkingDaysService @Inject()(val bankHolidaysConnector: BankHolidaysConnector) {

  //todo do this better no time now! I need a shower after writeing this!
  val test = BankHolidaySet("Hello",List.empty)

  def addWorkingDays(date:  LocalDate, days: Int): LocalDate = {
    implicit val hols: BankHolidaySet =
      Try {
        Await.result(bankHolidaysConnector.bankHolidays()(HeaderCarrier()), 5 seconds)
      }.getOrElse {
        Logger.error("Failed to load bank holidays schedule from BankHolidaysConnector, using default bank holiday set")
        test
      }
   //turn java to jodatime
    val joda =  org.joda.time.LocalDate.now().withMonthOfYear(date.getMonthValue).withDayOfMonth(date.getDayOfMonth).withYear(date.getYear)
    val jodaLocalDate: time.LocalDate = (joda).plusWorkingDays(days)
    LocalDate.now().withYear(jodaLocalDate.getYear).withDayOfMonth(jodaLocalDate.getDayOfMonth).withMonth(jodaLocalDate.getMonthOfYear)
  }
}
