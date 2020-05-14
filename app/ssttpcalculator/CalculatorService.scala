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

package ssttpcalculator

import java.time.LocalDate.now
import java.time.temporal.ChronoUnit.{DAYS, MONTHS}
import java.time.{Clock, LocalDate}

import bankholidays.WorkingDaysService.addWorkingDays
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.mvc.Request
import req.RequestSupport._
import ssttpcalculator.CalculatorService._
import times.ClockProvider
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, PaymentSchedule}
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal
import scala.util.{Failure, Success, Try}

class CalculatorService @Inject() (calculatorConnector: CalculatorConnector, clockProvider: ClockProvider)
  (implicit ec: ExecutionContext) {

  import clockProvider._

  def availablePaymentSchedules(sa: SelfAssessmentDetails, initialPayment: BigDecimal = BigDecimal(0))
    (implicit request: Request[_]): Future[List[PaymentSchedule]] = {

    JourneyLogger.info(s"CalculatorService.availablePaymentSchedules...")

    val rangeOfAvailableScheduleDurationsInMonths =
      minimumMonthsAllowedTTP to maximumDurationInMonths(sa, LocalDate.now(clockProvider.getClock))

    val debits = sa.debits.map(model.asDebitInput)

    Future.sequence(
      rangeOfAvailableScheduleDurationsInMonths.map { durationInMonths =>
        calculatorConnector.calculatePaymentSchedule(paymentScheduleRequest(debits, initialPayment, durationInMonths))
      }.toList
    )
  }
}

object CalculatorService {
  private val latestValidPaymentDayOfMonth = 28

  def payTodayRequest(debits: Seq[DebitInput])(implicit clock: Clock): CalculatorInput =
    changeScheduleRequest(0, now(clock).getDayOfMonth, 0, debits)

  def paymentScheduleRequest(debits: Seq[DebitInput], initialPayment: BigDecimal, durationInMonths: Int)
    (implicit clock: Clock): CalculatorInput = {

    val noInitialPayment = BigDecimal(0)
    val workingDaysInAWeek = 5

    val currentDate = now(clock)
    val endDate = currentDate.plusMonths(durationInMonths)
    val possibleFirstPaymentDate = addWorkingDays(currentDate, workingDaysInAWeek)

    val firstPaymentDate: LocalDate =
      if (possibleFirstPaymentDate.getDayOfMonth > latestValidPaymentDayOfMonth)
        possibleFirstPaymentDate.withDayOfMonth(1).plusMonths(1)
      else
        possibleFirstPaymentDate

    if (initialPayment > 0) {
      val deferredEndDate = endDate.plusMonths(1)
      val deferredFirstPaymentDate = firstPaymentDate.plusMonths(1)

      if ((debits.map(_.amount).sum - initialPayment) < BigDecimal.exact("32.00")) {
        CalculatorInput(
          startDate        = currentDate,
          initialPayment   = noInitialPayment,
          firstPaymentDate = Some(deferredFirstPaymentDate),
          endDate          = deferredEndDate,
          debits           = debits)
      } else {
        CalculatorInput(
          startDate        = currentDate,
          initialPayment   = initialPayment,
          firstPaymentDate = Some(deferredFirstPaymentDate),
          endDate          = deferredEndDate,
          debits           = debits)
      }
    } else
      CalculatorInput(
        startDate        = currentDate,
        initialPayment   = noInitialPayment,
        endDate          = endDate,
        firstPaymentDate = Some(firstPaymentDate),
        debits           = debits
      )
  }

  /**
   * As the user can change which day of the month they wish to make their payments, then a recalculation
   * must be made as this would effect the interest amounts. Rules here must be applied and this function
   * calculates the first payment date and the last payment date by applying these rules.
   *
   * Rules:
   * - First payment must be at least 7 days from today's date
   * - The day of the month cannot be greater than 28, if it is then use the 1st of the following month
   * - There must be at least a 14 day gap between the initial payment date and the first scheduled payment date
   */
  def changeScheduleRequest(
      durationInMonths: Int, preferredPaymentDayOfMonth: Int, initialPayment: BigDecimal, debits: Seq[DebitInput])
    (implicit clock: Clock): CalculatorInput = {

    val oneWeek = 7
    val twoWeeks = oneWeek * 2

    val startDate = now(clock)
    val startDatePlusOneWeek = startDate.plusWeeks(1)

    val bestMatchingPaymentDayOfMonth = if (preferredPaymentDayOfMonth > latestValidPaymentDayOfMonth) 1 else preferredPaymentDayOfMonth
    val defaultPaymentDate = startDate.withDayOfMonth(bestMatchingPaymentDayOfMonth)
    val defaultPaymentDatePlusOneMonth = defaultPaymentDate.plusMonths(1)
    val defaultPaymentDatePlusTwoMonths = defaultPaymentDate.plusMonths(2)

    val defaultEndDate = defaultPaymentDate.plusMonths(durationInMonths)
    val defaultEndDatePlusOneMonth = defaultEndDate.plusMonths(1)
    val defaultEndDatePlusTwoMonths = defaultEndDate.plusMonths(2)

    val (firstPaymentDate: LocalDate, endDate: LocalDate) =
      if (initialPayment.equals(BigDecimal(0))) {
        val daysBetweenStartAndPaymentDates = DAYS.between(startDate, defaultPaymentDate)

        if (daysBetweenStartAndPaymentDates < oneWeek && DAYS.between(startDate, defaultPaymentDatePlusOneMonth) < oneWeek)
          (defaultPaymentDatePlusTwoMonths, defaultEndDatePlusTwoMonths)
        else if (daysBetweenStartAndPaymentDates < oneWeek)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else
          (defaultPaymentDate, defaultEndDate)
      } else {
        val paymentDateWithinAWeekOfStartDate = defaultPaymentDate.isBefore(startDatePlusOneWeek)

        if (paymentDateWithinAWeekOfStartDate && DAYS.between(startDatePlusOneWeek, defaultPaymentDatePlusOneMonth) < twoWeeks)
          (defaultPaymentDatePlusTwoMonths, defaultEndDatePlusTwoMonths)
        else if (paymentDateWithinAWeekOfStartDate)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else if (DAYS.between(startDatePlusOneWeek, defaultPaymentDate) < twoWeeks)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else
          (defaultPaymentDate, defaultEndDate)
      }

    CalculatorInput(debits, initialPayment, startDate, endDate.minusDays(1), Some(firstPaymentDate))
  }

  val minimumMonthsAllowedTTP = 2

  implicit def ordered[A](implicit ev$1: A => Comparable[_ >: A]): Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }

  /*
  * Rules:
   a)    End of the calendar month before the due date of the next liability which is outside of the TTP We are not checking this rule
   b)    End of the calendar month before the due date of the next non-submitted SA return
   c)    12 months from the earliest due date of the amounts included in the TTP (*ignoring due dates for any amounts under £32)
    */
  def maximumDurationInMonths(sa: SelfAssessmentDetails, today: LocalDate)(implicit hc: HeaderCarrier): Int = Try {
      def max(date1: LocalDate, date2: LocalDate) = if (math.Ordering[LocalDate].gt(date1, date2)) date1 else date2
      def min(date1: LocalDate, date2: LocalDate) = if (math.Ordering[LocalDate].lt(date1, date2)) date1 else date2
      def monthsBetween(date1: LocalDate, date2: LocalDate) = MONTHS.between(date1, date2).toInt

      def lastDayOfPreviousMonth(date: LocalDate) = {
        val lastMonth = date.minusMonths(1)
        lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
      }

    val maybeSubmissionDate = sa.returns.flatMap(_.dueDate.map(_.plusYears(1))).reduceOption(max)
    val maybeDueDate = sa.debits.filter(_.amount > 32).map(_.dueDate.plusYears(1)).reduceOption(min)

    val maximumAllowedDurationInMonths = 11

    val maximumDurationInMonthsBasedOnSubmissionDate =
      maybeSubmissionDate
        .map(submissionDate => monthsBetween(today, lastDayOfPreviousMonth(submissionDate)))
        .getOrElse(maximumAllowedDurationInMonths)

    val maximumDurationInMonthsBasedOnDueDate =
      maybeDueDate.map(dueDate => monthsBetween(today, dueDate)).getOrElse(maximumAllowedDurationInMonths)

    val maximumDurationInMonths =
      Math.min(
        maximumAllowedDurationInMonths,
        Math.min(maximumDurationInMonthsBasedOnSubmissionDate, maximumDurationInMonthsBasedOnDueDate))

    JourneyLogger.info(
      s"""
         |maximumDurationInMonths = $maximumDurationInMonths
         |todayDate = $today
         |maybeSubmissionDate = $maybeSubmissionDate
         |maximumDurationInMonthsBasedOnSubmissionDate = $maximumDurationInMonthsBasedOnSubmissionDate
         |maximumDurationInMonthsBasedOnDueDate = $maximumDurationInMonthsBasedOnDueDate
         |""".stripMargin,
      sa
    )

    maximumDurationInMonths
  } match {
    case Success(s) => s
    case Failure(e) =>
      Logger.error(s"calculateGapInMonths failed. [todayDate=$today], selfAssessment:\n${toJson(sa.obfuscate)}", e)
      throw e
  }
}

