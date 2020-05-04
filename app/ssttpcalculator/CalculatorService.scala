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

import java.time.temporal.ChronoUnit.{DAYS, MONTHS}
import java.time.{Clock, LocalDate}

import bankholidays.WorkingDaysService
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

class CalculatorService @Inject() (
    calculatorConnector: CalculatorConnector,
    workingDays:         WorkingDaysService,
    clockProvider:       ClockProvider)(
    implicit
    ec: ExecutionContext) {

  import clockProvider._

  //todo perhaps merge these methods and change back end so it only one call
  //TODO why this method returns list of schedules by month? WTF?
  def getInstalmentsSchedule(sa: SelfAssessmentDetails, initialPayment: BigDecimal = BigDecimal(0))
    (implicit request: Request[_]): Future[List[PaymentSchedule]] = {

    JourneyLogger.info(s"CalculatorService.getInstalmentsSchedule...")

    val months: Seq[Int] = getMonthRange(sa)

    val calculatorInputs = months.map { month: Int =>
      val calculatorInput: CalculatorInput = createCalculatorInput(
        durationMonths = month,
        dayOfMonth     = LocalDate.now(clockProvider.getClock).getDayOfMonth,
        initialPayment = initialPayment,
        debits         = sa.debits.map(model.asDebitInput)
      )
      validateCalculatorDates(calculatorInput, month, sa.debits.map(model.asDebitInput))
    }.toList

    getCalculatorValues(calculatorInputs)
  }

  private def getCalculatorValues(calculatorInputs: List[CalculatorInput])(implicit request: Request[_]) = {
    JourneyLogger.info(s"CalculatorService.getCalculatorValues...")

    Future.sequence(calculatorInputs.map { calculatorInput =>
      calculatorConnector.calculatePaymentSchedule(calculatorInput)
    })
  }

  private def dayOfMonthCheck(date: LocalDate): LocalDate = date.getDayOfMonth match {
    case day if day > 28 => date.withDayOfMonth(1).plusMonths(1)
    case _               => date
  }

  //TODO: describe in words 'why'  and `WHAT` it does!?
  //why it takes CalcInput and DebitInputs !?
  private def validateCalculatorDates(
      calculatorInput: CalculatorInput,
      numberOfMonths:  Int,
      debits:          Seq[DebitInput]
  )(implicit request: Request[_]): CalculatorInput = {
    val workingDaysInAWeek = 5
    val firstPaymentDate: LocalDate = dayOfMonthCheck(workingDays.addWorkingDays(clockProvider.nowDate(), workingDaysInAWeek))

    if (calculatorInput.initialPayment > 0) {
      if ((debits.map(_.amount).sum - calculatorInput.initialPayment) < BigDecimal.exact("32.00")) {
        calculatorInput.copy(startDate        = clockProvider.nowDate(),
                             initialPayment   = BigDecimal(0),
                             firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusMonths(1))),
                             endDate          = calculatorInput.startDate.plusMonths(numberOfMonths + 1))
      } else {
        calculatorInput.copy(startDate        = clockProvider.nowDate(),
                             firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusMonths(1))),
                             endDate          = calculatorInput.startDate.plusMonths(numberOfMonths + 1))
      }
    } else
      calculatorInput.copy(
        debits           = debits,
        startDate        = clockProvider.nowDate(),
        endDate          = calculatorInput.startDate.plusMonths(numberOfMonths),
        firstPaymentDate = Some(firstPaymentDate)
      )
  }
}

object CalculatorService {
  /**
   * Applies the 7 and 14 day rules for the calculator page, using today's date.
   * See the function createCalculatorInput in Arrangement Controller for further information
   */
  private def checkDayOfMonth(dayOfMonth: Int): Int = dayOfMonth match {
    case day if day > 28 => 1
    case _               => dayOfMonth
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
  //todo write test's I am too afraid to touch this !
  def createCalculatorInput(
      durationMonths: Int,
      dayOfMonth:     Int,
      initialPayment: BigDecimal      = BigDecimal(0),
      debits:         Seq[DebitInput])(implicit clock: Clock): CalculatorInput = {

    val startDate = LocalDate.now(clock)

    val initialDate = startDate.withDayOfMonth(checkDayOfMonth(dayOfMonth))
    val daysInWeek = 7
    val (firstPaymentDate: LocalDate, lastPaymentDate: LocalDate) = if (initialPayment.equals(BigDecimal(0))) {
      if (DAYS.between(startDate, initialDate) < daysInWeek && DAYS.between(startDate, initialDate.plusMonths(1)) < daysInWeek)
        (initialDate.plusMonths(2), initialDate.plusMonths(durationMonths + 2).minusDays(1))
      else if (DAYS.between(startDate, initialDate) < 7)
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
      else
        (initialDate, initialDate.plusMonths(durationMonths).minusDays(1))
    } else {
      if (initialDate.isBefore(startDate.plusWeeks(1)) && DAYS.between(startDate.plusWeeks(1), initialDate.plusMonths(1)) < 14)
        (initialDate.plusMonths(2), initialDate.plusMonths(durationMonths + 2).minusDays(1))
      else if (initialDate.isBefore(startDate.plusWeeks(1)))
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
      else if (DAYS.between(startDate.plusWeeks(1), initialDate) < 14)
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
      else
        (initialDate, initialDate.plusMonths(durationMonths).minusDays(1))
    }

    CalculatorInput(
      debits           = debits,
      initialPayment   = initialPayment,
      startDate        = startDate,
      endDate          = lastPaymentDate,
      firstPaymentDate = Some(firstPaymentDate)
    )
  }

  val minimumMonthsAllowedTTP = 2

  def getMonthRange(selfAssessment: SelfAssessmentDetails)(implicit request: Request[_], clock: Clock): Seq[Int] = {
    val range = minimumMonthsAllowedTTP to maximumDurationInMonths(selfAssessment, LocalDate.now(clock))
    JourneyLogger.info(s"getMonthRange: [months=$range]")
    range
  }

  implicit def ordered[A](implicit ev$1: A => Comparable[_ >: A]): Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }

  /*
  * Rules:
   a)    End of the calendar month before the due date of the next liability which is outside of the TTP We are not checking this rule
   b)    End of the calendar month before the due date of the next non-submitted SA return
   c)    12 months from the earliest due date of the amounts included in the TTP (*ignoring due dates for any amounts under £32)
    */
  def maximumDurationInMonths(selfAssessment: SelfAssessmentDetails, today: LocalDate)(implicit hc: HeaderCarrier): Int = Try {
      def max(date1: LocalDate, date2: LocalDate) = if (math.Ordering[LocalDate].gt(date1, date2)) date1 else date2
      def min(date1: LocalDate, date2: LocalDate) = if (math.Ordering[LocalDate].lt(date1, date2)) date1 else date2
      def monthsBetween(date1: LocalDate, date2: LocalDate) = MONTHS.between(date1, date2).toInt

      def lastDayOfPreviousMonth(date: LocalDate) = {
        val lastMonth = date.minusMonths(1)
        lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
      }

    val maybeSubmissionDate = selfAssessment.returns.flatMap(_.dueDate.map(_.plusYears(1))).reduceOption(max)
    val maybeDueDate = selfAssessment.debits.filter(_.amount > 32).map(_.dueDate.plusYears(1)).reduceOption(min)

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
      selfAssessment
    )

    maximumDurationInMonths
  } match {
    case Success(s) => s
    case Failure(e) =>
      Logger.error(s"calculateGapInMonths failed. [todayDate=$today], selfAssessment:\n${toJson(selfAssessment.obfuscate)}", e)
      throw e
  }
}

