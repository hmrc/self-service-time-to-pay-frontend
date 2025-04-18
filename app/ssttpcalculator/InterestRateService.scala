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

package ssttpcalculator

import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Singleton
import ssttpcalculator.model.InterestRate
import util.Logging

import javax.inject.Inject
import play.api.mvc.Request
import times.ClockProvider
import timetopaytaxpayer.cor.model.SelfAssessmentDetails

import java.util.Locale
import scala.io.Source

@Singleton
class InterestRateService @Inject() (clockProvider: ClockProvider) extends Logging {

  lazy val rates: Seq[InterestRate] = streamInterestRates()
  val filename: String = "/interestRates.csv"
  val source: Source = Source.fromInputStream(getClass.getResourceAsStream(filename))
  val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)

  private def interestRateConsumer(rates: Seq[InterestRate], line: String): Seq[InterestRate] = {
    line.split(",").toSeq match {
      case Seq(date, rate) =>
        val endDate: LocalDate = rates.lastOption.map(ir => ir.startDate.minusDays(1)).getOrElse(LocalDate.MAX)
        rates :+ InterestRate(LocalDate.parse(date, DATE_TIME_FORMATTER), endDate, BigDecimal(rate))
      case _ => throw new IndexOutOfBoundsException()
    }
  }

  private val streamInterestRates: () => Seq[InterestRate] = { () =>
    try {
      source.getLines().foldLeft(Seq[InterestRate]())(interestRateConsumer)
    } catch {
      case _: NullPointerException => throw new FileNotFoundException(s"$source")
      case t: Throwable            => throw t
    }
  }

  def rateOn(date: LocalDate): InterestRate = {
    rates.find(rate => rate.startDate.compareTo(date) <= 0).getOrElse(
      throw new RuntimeException(s"It should not happen. This date is too old. There is no rate defined for it. [date:$date] [rates:$rates]")
    )
  }

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  implicit def orderingInterestRate: Ordering[InterestRate] = Ordering.by(_.startDate)

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def getRatesForPeriod(startDate: LocalDate, endDate: LocalDate): Seq[InterestRate] = {
    rates.filter { interestRate =>
      (interestRate.startDate.compareTo(endDate) <= 0) &&
        (interestRate.endDate.compareTo(startDate) >= 0)
    }.sorted.flatMap { rate =>
      val startYear = Seq(rate.startDate.getYear, startDate.getYear).max
      val endYear = Seq(rate.endDate.getYear, endDate.getYear).min

      Range.inclusive(startYear, endYear).map { year =>
        val ir = InterestRate(
          startDate = Seq(LocalDate.of(year, 1, 1), startDate, rate.startDate).max,
          endDate   =
            Seq(
              LocalDate.of(year, 12, 31),
              endDate,
              rate.endDate
            ).min,
          rate      = rate.rate
        )
        contextlessLogger("interest rates").info(s"Rate: $ir")
        ir
      }
    }
  }

  def applicableInterestRates(sa: SelfAssessmentDetails)(implicit request: Request[_]): Seq[InterestRate] = {
    val firstDueDate = sa.debits.map(_.dueDate).min
    val today: LocalDate = clockProvider.nowDate()
    if (today isBefore firstDueDate) getRatesForPeriod(today, today)
    else getRatesForPeriod(firstDueDate, today)
  }

}
