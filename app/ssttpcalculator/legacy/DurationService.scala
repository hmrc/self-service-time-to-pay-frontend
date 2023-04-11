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

package ssttpcalculator.legacy

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit._

import javax.inject.Singleton

@Singleton
class DurationService {
  def getDaysBetween(startDate: LocalDate, endDate: LocalDate, inclusive: Boolean = true): Long = calculatePeriod(startDate, endDate, DAYS, inclusive)

  /**
   * Calculates the number of days between two dates which uses the inclusive flag to determine if it is single
   * inclusive (includes the end date) or not inclusive (excludes both start and end date).
   */
  private def calculatePeriod(startDate: LocalDate, endDate: LocalDate, frequency: ChronoUnit, inclusive: Boolean): Long = {
    frequency.between(startDate, endDate) + (if (inclusive) 0 else -1) match {
      case c if c > 0 => c
      case _          => 0
    }
  }

  def getRepaymentDates(startDate: LocalDate, endDate: LocalDate): Seq[LocalDate] = {
    if (startDate.isAfter(endDate)) throw new IllegalArgumentException("Start date must be BEFORE end date")

    Iterator.iterate(startDate)(_ plusMonths 1).takeWhile(_.compareTo(endDate) <= 0).toSeq
  }
}
