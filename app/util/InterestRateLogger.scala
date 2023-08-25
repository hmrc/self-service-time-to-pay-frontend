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

package util

import journey.Journey
import play.api.mvc.Request
import ssttpcalculator.model.InterestRate

import java.time.LocalDate

trait InterestRateLogger { self: JourneyLogger =>
  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def debug(message: => String, interestRates: Seq[InterestRate])(implicit request: Request[_], journey: Journey): Unit = {
    applicableInterestRateLogMessages(interestRates).foreach(rateMessage => debug(message + rateMessage))
  }

  def info(message: => String, interestRates: Seq[InterestRate])(implicit request: Request[_], journey: Journey): Unit = {
    applicableInterestRateLogMessages(interestRates).foreach(rateMessage => info(message + rateMessage))
  }

  def warn(message: => String, interestRates: Seq[InterestRate])(implicit request: Request[_], journey: Journey): Unit = {
    applicableInterestRateLogMessages(interestRates).foreach(rateMessage => warn(message + rateMessage))
  }

  def error(message: => String, interestRates: Seq[InterestRate])(implicit request: Request[_], journey: Journey): Unit = {
    applicableInterestRateLogMessages(interestRates).foreach(rateMessage => error(message + rateMessage))
  }

  def applicableInterestRateLogMessages(interestRates: Seq[InterestRate]): Seq[String] = {
    val interestRatesSorted = interestRates.sortBy(_.endDate)

    interestRatesSorted.length match {
      case 0 => Seq()
      case 1 =>
        Seq(s"[from ${interestRatesSorted.last.startDate} onwards => ${interestRatesSorted.last.rate}%]")
      case _ =>
        interestRatesSorted.init.map { rate =>
          s"[from ${rate.startDate} to ${rate.endDate} => ${rate.rate}%]"
        } :+ s"[from ${interestRatesSorted.last.startDate} onwards => ${interestRatesSorted.last.rate}%]"
    }
  }
}
