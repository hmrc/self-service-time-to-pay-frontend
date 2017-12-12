/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.DAYS

import play.api.Logger
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.math.BigDecimal

object CalculatorLogic {
  /**
    * Applies the 7 and 14 day rules for the calculator page, using today's date.
    * See the function createCalculatorInput in Arrangement Controller for further information
    */
  //todo try and refactor
  private def dayOfMonthCheck(date: LocalDate): LocalDate = date.getDayOfMonth match {
    case day if day > 28 => date.withDayOfMonth(1).plusMonths(1)
    case _ => date
  }

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
  def createCalculatorInput(ttpSubmission: TTPSubmission, dayOfMonth: Int): Option[CalculatorInput] = {
    val startDate = LocalDate.now()
    val durationMonths = ttpSubmission.durationMonths.getOrElse(maxAllowedMonthlyInstalments)
    val initialDate = startDate.withDayOfMonth(checkDayOfMonth(dayOfMonth))
    val daysInWeek = 7
    val (firstPaymentDate: LocalDate, lastPaymentDate: LocalDate) = if (ttpSubmission.calculatorData.initialPayment.equals(BigDecimal(0))) {
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

    Some(ttpSubmission.calculatorData.copy(
      startDate = startDate,
      firstPaymentDate = Some(firstPaymentDate),
      endDate = lastPaymentDate,
      initialPayment = ttpSubmission.schedule.get.initialPayment
    ))
  }

  /*
    * Rules:
      * - First payment must be at least 7 days from today's date
    * - The day of the month cannot be greater than 28, if it is then use the 1st of the following month
    * - There must be at least a 14 day gap between the initial payment date and the first scheduled payment date
      */
  val maxAllowedMonthlyInstalments = 11

  //todo look at naming I am on auto pilot here!!!
  private def monthDifference(date: LocalDate, dateToCompareTo: LocalDate) = ChronoUnit.MONTHS.between(date, dateToCompareTo).toInt

  private def endOfMonthBeforeLastDay(date: LocalDate): LocalDate = {
    val lastMonth = date.minusMonths(1)
    lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
  }

  implicit def ordered[A <% Comparable[_ >: A]]: Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }

  def setMaxMonthsAllowed(selfAssessment: Option[SelfAssessment] = None, todaysDate: LocalDate): Int = {

    selfAssessment.fold(maxAllowedMonthlyInstalments)(sa => calculateGapInMonths(sa, todaysDate))
  }

  def getFutureReturn(returnedFiled: List[Return]): Option[LocalDate] = {
    if (returnedFiled.isEmpty) None else Option(returnedFiled.flatMap(_.dueDate.map(_.plusYears(1))).max)
  }

  /*
  * Rules:
   a)    End of the calendar month before the due date of the next liability which is outside of the TTP
   b)    End of the calendar month before the due date of the next non-submitted SA return
   c)    12 months from the earliest due date of the amounts included in the TTP (*ignoring due dates for any amounts under £32)
    */
  def calculateGapInMonths(selfAssessment: SelfAssessment, todayDate: LocalDate): Int = {
    //todo this code needs a serious refactoring
    val endOfTaxYearMonthDay = (4, 6)

    def beforeAprilSixth(date: LocalDate): Boolean = date.isBefore(date.withMonth(endOfTaxYearMonthDay._1).withDayOfMonth(endOfTaxYearMonthDay._2))

    //todo clean up too be pretty
    val currentTaxYear = if (beforeAprilSixth(todayDate)) todayDate.withMonth(endOfTaxYearMonthDay._1).withDayOfMonth(endOfTaxYearMonthDay._2)
    else todayDate.plusYears(1).withMonth(endOfTaxYearMonthDay._1).withDayOfMonth(endOfTaxYearMonthDay._2)


    val (currentLiabilities, futureLiabilities): (Seq[Debit], Seq[Debit]) =
      selfAssessment.debits.partition(d => d.taxYearEnd.exists(_.isBefore(currentTaxYear.minusYears(1))))

    val futureLiabilitiesDueDateEarliest: Option[LocalDate] = if (futureLiabilities.isEmpty) None else Option(futureLiabilities.map(_.dueDate).min)

    val nextSubmissionReturn: Option[LocalDate] = selfAssessment.returns.flatMap(getFutureReturn)


    val yearAfterEarliestCurrantLiabilities: Option[LocalDate] = if (currentLiabilities.exists(_.amount < 32)) None else
      Option(currentLiabilities.map(_.dueDate).min.plusYears(1))

    val differenceFutureLiabilities: Int = futureLiabilitiesDueDateEarliest.map(fl =>
      monthDifference(todayDate, endOfMonthBeforeLastDay(fl))).getOrElse(maxAllowedMonthlyInstalments)

    val differenceNextSubmissionReturn: Int = nextSubmissionReturn.map(sr =>
      monthDifference(todayDate, endOfMonthBeforeLastDay(sr))).getOrElse(maxAllowedMonthlyInstalments)

    val differenceEarliestDueDate: Int = yearAfterEarliestCurrantLiabilities.map(el =>
      monthDifference(todayDate, el)).getOrElse(maxAllowedMonthlyInstalments)

    List(differenceFutureLiabilities, differenceNextSubmissionReturn, differenceEarliestDueDate).min
  }

  def validateCalculatorDates(calculatorInput: CalculatorInput, numberOfMonths: Int, debits: Seq[Debit]): CalculatorInput = {
    val firstPaymentDate = dayOfMonthCheck(LocalDate.now().plusWeeks(1))
    if (calculatorInput.initialPayment > 0) {
      if (debits.map(_.amount).sum.-(calculatorInput.initialPayment) < BigDecimal.exact("32.00")) {
        calculatorInput.copy(startDate = LocalDate.now,
          initialPayment = BigDecimal(0),
          firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusWeeks(2))),
          endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
      } else {
        calculatorInput.copy(startDate = LocalDate.now,
          firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusWeeks(2))),
          endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
      }
    }
    else
      calculatorInput.copy(startDate = LocalDate.now(),
        firstPaymentDate = Some(firstPaymentDate),
        endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
  }
}
