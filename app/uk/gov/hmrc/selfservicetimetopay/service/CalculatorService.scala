/*
 * Copyright 2019 HM Revenue & Customs
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
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.DAYS

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors.CalculatorConnector
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.service.CalculatorService.{createCalculatorInput, getMonthRange, _}

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal

class CalculatorService @Inject()(calculatorConnector: CalculatorConnector,
                                  workingDays: WorkingDaysService) {

  //todo perhaps merge these methods and change back end so it only one call
  def getInstalmentsSchedule(sa: SelfAssessment, intialPayment: BigDecimal = BigDecimal(0))
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Map[Int, CalculatorPaymentSchedule]] = {
    val input: List[(Int, CalculatorInput)] = getMonthRange(sa).map(month => {
      val calculatorInput = createCalculatorInput(month, LocalDate.now().getDayOfMonth, intialPayment, sa.debits)
      (month, validateCalculatorDates(calculatorInput, month, sa.debits))
    }).toList

    getCalculatorValues(input)
  }
  def getInstalmentsScheduleUnAuth(debits: Seq[Debit])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Map[Int, CalculatorPaymentSchedule]] = {

    val input: List[(Int, CalculatorInput)] =  (minimumMonthsAllowedTTP to maxAllowedMonthlyInstalments).map(month => {
      val caltInput = createCalculatorInput(month, LocalDate.now().getDayOfMonth, 0, debits)
      (month, validateCalculatorDates(caltInput, month, debits))
    }).toList
    getCalculatorValues(input)
  }

  private def getCalculatorValues(inputs:List[(Int, CalculatorInput)])(implicit hc: HeaderCarrier, ec: ExecutionContext) ={
    val futureSchedules: Seq[Future[(Int, CalculatorPaymentSchedule)]] = inputs.map {
      case (numberOfMonths, calcInput) =>
        calculatorConnector.calculatePaymentSchedule(calcInput).map(x => (numberOfMonths, x.head))
    }
    val returnedValues: Future[Seq[(Int, CalculatorPaymentSchedule)]] = Future.sequence(futureSchedules)
    returnedValues.map(a => ListMap(a.sortBy(_._1): _*))
  }

  private def dayOfMonthCheck(date: LocalDate): LocalDate = date.getDayOfMonth match {
    case day if day > 28 => date.withDayOfMonth(1).plusMonths(1)
    case _ => date
  }

  private def validateCalculatorDates(calculatorInput: CalculatorInput, numberOfMonths: Int, debits: Seq[Debit]): CalculatorInput = {
    val workingDaysInAWeek = 5
    val firstPaymentDate: LocalDate = dayOfMonthCheck(workingDays.addWorkingDays(LocalDate.now(), workingDaysInAWeek))
    if (calculatorInput.initialPayment > 0) {
      if ((debits.map(_.amount).sum - calculatorInput.initialPayment) < BigDecimal.exact("32.00")) {
        calculatorInput.copy(startDate = LocalDate.now,
          initialPayment = BigDecimal(0),
          firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusMonths(1))),
          endDate = calculatorInput.startDate.plusMonths(numberOfMonths + 1))
      } else {
        calculatorInput.copy(startDate = LocalDate.now,
          firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusMonths(1))),
          endDate = calculatorInput.startDate.plusMonths(numberOfMonths + 1))
      }
    }
    else
      calculatorInput.copy(startDate = LocalDate.now(),
        firstPaymentDate = Some(firstPaymentDate),
        endDate = calculatorInput.startDate.plusMonths(numberOfMonths),
        debits = debits)
  }
}

object CalculatorService {
  /**
    * Applies the 7 and 14 day rules for the calculator page, using today's date.
    * See the function createCalculatorInput in Arrangement Controller for further information
    */
  private def checkDayOfMonth(dayOfMonth: Int): Int = dayOfMonth match {
    case day if day > 28 => 1
    case _ => dayOfMonth
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
  def createCalculatorInput(durationMonths: Int, dayOfMonth: Int, initialPayment: BigDecimal = BigDecimal(0), debits: Seq[Debit]): CalculatorInput = {
    val startDate = LocalDate.now()

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
    CalculatorInput(debits, initialPayment, startDate, lastPaymentDate, Some(firstPaymentDate))
  }

  val minimumMonthsAllowedTTP = 2
  val maxAllowedMonthlyInstalments = 11

  //todo look at naming I am on auto pilot here!!!
  private def monthDifference(date: LocalDate, dateToCompareTo: LocalDate) = ChronoUnit.MONTHS.between(date, dateToCompareTo).toInt

  private def endOfMonthBeforeLastDay(date: LocalDate): LocalDate = {
    val lastMonth = date.minusMonths(1)
    lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
  }

  def getMonthRange(selfAssessment: SelfAssessment): Seq[Int] =
    minimumMonthsAllowedTTP to getMaxMonthsAllowed(selfAssessment, LocalDate.now())

  implicit def ordered[A <% Comparable[_ >: A]]: Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }

  def getMaxMonthsAllowed(selfAssessment: SelfAssessment, todaysDate: LocalDate): Int = {

    calculateGapInMonths(selfAssessment, todaysDate)
  }

  def getFutureReturn(returnedFiled: List[Return]): Option[LocalDate] = {
    val returnsDueDateFuture = returnedFiled.flatMap(_.dueDate.map(_.plusYears(1)))
    if (returnsDueDateFuture.isEmpty) None else Option(returnsDueDateFuture.max)
  }

  /*
  * Rules:
   a)    End of the calendar month before the due date of the next liability which is outside of the TTP We are not checking this rule
   b)    End of the calendar month before the due date of the next non-submitted SA return
   c)    12 months from the earliest due date of the amounts included in the TTP (*ignoring due dates for any amounts under £32)
    */
  def calculateGapInMonths(selfAssessment: SelfAssessment, todayDate: LocalDate): Int = {

    val nextSubmissionReturn: Option[LocalDate] = selfAssessment.returns.flatMap(getFutureReturn)

    val yearAfterEarliestCurrantLiabilities: Option[LocalDate] =
      Option(selfAssessment.debits.filter(_.amount > 32).map(_.dueDate).min.plusYears(1))

    val differenceNextSubmissionReturn: Int = nextSubmissionReturn.map(sr =>
      monthDifference(todayDate, endOfMonthBeforeLastDay(sr))).getOrElse(maxAllowedMonthlyInstalments)

    val differenceEarliestDueDate: Int = yearAfterEarliestCurrantLiabilities.map(el =>
      monthDifference(todayDate, el)).getOrElse(maxAllowedMonthlyInstalments)

    val smallestDifferance = List(differenceNextSubmissionReturn, differenceEarliestDueDate).min
    if (smallestDifferance > maxAllowedMonthlyInstalments) maxAllowedMonthlyInstalments else smallestDifferance
  }
}

