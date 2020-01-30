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

import java.time.{Clock, LocalDate}
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.DAYS

import bankholidays.WorkingDaysService
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import ssttpcalculator.CalculatorService._
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, PaymentSchedule}
import timetopaytaxpayer.cor.model.{Debit, Return, SelfAssessmentDetails}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal
import scala.util.{Failure, Success, Try}
import req.RequestSupport._

class CalculatorService @Inject() (
    calculatorConnector: CalculatorConnector,
    workingDays:         WorkingDaysService)(
    implicit
    ec:    ExecutionContext,
    clock: Clock) {

  //todo perhaps merge these methods and change back end so it only one call
  //TODO why this method returns list of schedules by month? WTF?
  def getInstalmentsSchedule(
      sa:            SelfAssessmentDetails,
      intialPayment: BigDecimal            = BigDecimal(0)
  )(
      implicit
      request: Request[_]
  ): Future[List[CalculatorPaymentScheduleExt]] = {

    JourneyLogger.info(s"CalculatorService.getInstalmentsSchedule...")

    val months: Seq[Int] = getMonthRange(sa)

    val input: List[(Int, CalculatorInput)] = months.map{ month: Int =>
      val calculatorInput: CalculatorInput = createCalculatorInput(month, LocalDate.now(clock).getDayOfMonth, intialPayment, sa.debits.map(model.asDebitInput))
      val calculatorInputValidated: CalculatorInput = validateCalculatorDates(calculatorInput, month, sa.debits.map(model.asDebitInput))

      (month, calculatorInputValidated)
    }.toList

    getCalculatorValues(input)
  }

  private def getCalculatorValues(inputs: List[(Int, CalculatorInput)])(implicit request: Request[_]) = {
    JourneyLogger.info(s"CalculatorService.getCalculatorValues...")
    val futureSchedules: List[Future[CalculatorPaymentScheduleExt]] = inputs.map {
      case (numberOfMonths, calcInput) =>
        calculatorConnector.calculatePaymentSchedule(calcInput)
          .map(x => CalculatorPaymentScheduleExt(numberOfMonths, x))
    }
    Future.sequence(futureSchedules)
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
  ): CalculatorInput = {
    val workingDaysInAWeek = 5
    val firstPaymentDate: LocalDate = dayOfMonthCheck(workingDays.addWorkingDays(LocalDate.now(clock), workingDaysInAWeek))

    if (calculatorInput.initialPayment > 0) {
      if ((debits.map(_.amount).sum - calculatorInput.initialPayment) < BigDecimal.exact("32.00")) {
        calculatorInput.copy(startDate        = LocalDate.now(clock),
                             initialPayment   = BigDecimal(0),
                             firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusMonths(1))),
                             endDate          = calculatorInput.startDate.plusMonths(numberOfMonths + 1))
      } else {
        calculatorInput.copy(startDate        = LocalDate.now(clock),
                             firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusMonths(1))),
                             endDate          = calculatorInput.startDate.plusMonths(numberOfMonths + 1))
      }
    } else
      calculatorInput.copy(
        debits           = debits,
        startDate        = LocalDate.now(clock),
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
  val maxAllowedMonthlyInstalments = 11

  //todo look at naming I am on auto pilot here!!!
  private def monthDifference(date: LocalDate, dateToCompareTo: LocalDate) = ChronoUnit.MONTHS.between(date, dateToCompareTo).toInt

  private def endOfMonthBeforeLastDay(date: LocalDate): LocalDate = {
    val lastMonth = date.minusMonths(1)
    lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
  }

  def getMonthRange(selfAssessment: SelfAssessmentDetails)(implicit request: Request[_], clock: Clock): Seq[Int] = {
    val range = minimumMonthsAllowedTTP to getMaxMonthsAllowed(selfAssessment, LocalDate.now(clock))
    JourneyLogger.info(s"getMonthRange: [months=$range]")
    range
  }

  implicit def ordered[A <% Comparable[_ >: A]]: Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }

  def getMaxMonthsAllowed(selfAssessment: SelfAssessmentDetails, todaysDate: LocalDate)(implicit request: Request[_]): Int = {

    calculateGapInMonths(selfAssessment, todaysDate)
  }

  def getFutureReturn(returnedFiled: Seq[Return]): Option[LocalDate] = {
    val returnsDueDateFuture = returnedFiled.flatMap(_.dueDate.map(_.plusYears(1)))
    if (returnsDueDateFuture.isEmpty) None else Option(returnsDueDateFuture.max)
  }

  /*
  * Rules:
   a)    End of the calendar month before the due date of the next liability which is outside of the TTP We are not checking this rule
   b)    End of the calendar month before the due date of the next non-submitted SA return
   c)    12 months from the earliest due date of the amounts included in the TTP (*ignoring due dates for any amounts under £32)
    */
  def calculateGapInMonths(selfAssessment: SelfAssessmentDetails, todayDate: LocalDate)(implicit headerCarrier: HeaderCarrier): Int = Try{

    val nextSubmissionReturn: Option[LocalDate] = getFutureReturn(selfAssessment.returns)

    val yearAfterEarliestCurrantLiabilities: Option[LocalDate] =
      Option(
        selfAssessment
          .debits
          .filter(_.amount > 32)
          .map(_.dueDate)
          .min
          .plusYears(1)
      )

    val differenceNextSubmissionReturn: Int = nextSubmissionReturn.map(sr =>
      monthDifference(todayDate, endOfMonthBeforeLastDay(sr))).getOrElse(maxAllowedMonthlyInstalments)

    val differenceEarliestDueDate: Int = yearAfterEarliestCurrantLiabilities.map(el =>
      monthDifference(todayDate, el)).getOrElse(maxAllowedMonthlyInstalments)

    val smallestDifferance = List(differenceNextSubmissionReturn, differenceEarliestDueDate).min
    val gap = if (smallestDifferance > maxAllowedMonthlyInstalments) maxAllowedMonthlyInstalments else smallestDifferance

    JourneyLogger.info(
      s"""
         |gap in months = $gap
         |todayDate = $todayDate
         |nextSubmissionReturn = $nextSubmissionReturn
         |differenceNextSubmissionReturn = $differenceNextSubmissionReturn
         |differenceEarliestDueDate = $differenceEarliestDueDate
         |smallestDifferance = $smallestDifferance
         |""".stripMargin,
      selfAssessment
    )
    gap
  } match {
    case Success(s) => s
    case Failure(e) =>
      import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
      Logger.error(s"calculateGapInMonths failed. [todayDate=$todayDate], selfAssessment:\n${Json.toJson(selfAssessment.obfuscate)}", e)
      throw e
  }
}

