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

package ssttpeligibility

import java.time.{LocalDate, MonthDay}

import timetopaytaxpayer.cor.model.{Debit, Return, SelfAssessmentDetails, Taxpayer}
import uk.gov.hmrc.selfservicetimetopay.models._

/**
 * Determines if a tax payer is eligible for self service.
 *
 * Terminology:
 * Debt - any money owed that is 30 days or more overdue
 * Charge - any money owed that less than 30 days overdue
 * Liability - any money that is not yet due
 */
object EligibilityService {
  val insignificantDebtUpperLimit = 32
  val maximumDebtForSelfServe = 10000
  val numberOfDaysAfterDueDateForDebtToBeConsideredOld = 60
  val returnHistoryYearsRequired = 4
  val taxYearEndDay: MonthDay = MonthDay.of(4, 5)

  def checkEligibility(dateOfEligibilityCheck: LocalDate, taxpayer: Taxpayer, directDebits: DirectDebitInstructions, onIa: Boolean): EligibilityStatus = {
    val selfAssessmentDetails: SelfAssessmentDetails = taxpayer.selfAssessment
    val isOnIa: List[Reason] = if (onIa) Nil else List(IsNotOnIa)

    val reasons =
      checkReturnsUpToDate(selfAssessmentDetails.returns, dateOfEligibilityCheck) ++
        checkDebits(selfAssessmentDetails.debits, dateOfEligibilityCheck) ++
        isOnIa ++
        hasDirectDebitsCreatedWithinTheLastYear(dateOfEligibilityCheck, directDebits)

    reasons match {
      case Nil => EligibilityStatus(Seq.empty)
      case _   => EligibilityStatus(reasons.map(r => r))
    }
  }

  private def hasDirectDebitsCreatedWithinTheLastYear(date: LocalDate, directDebits: DirectDebitInstructions): Seq[Reason] = {
    val directDebitsCreatedWithinTheLastYear =
      directDebits.directDebitInstruction.exists(ddi =>
        ddi.creationDate.getOrElse(date).isAfter(date.minusYears(1)))

    if (directDebitsCreatedWithinTheLastYear) Seq(DirectDebitCreatedWithinTheLastYear) else Seq.empty
  }

  private def checkReturnsUpToDate(returns: Seq[Return], today: LocalDate): List[Reason] = {
    val currentTaxYearEndDate: LocalDate = taxYearEndDateForCalendarYear(today)
    //This is a bit complex but basically it just looks at the last *returnHistoryYearsRequired* years of tax returns
    //and checks to see if they have all been filed
    (0 to returnHistoryYearsRequired).reverse
      .map(currentTaxYearEndDate.getYear - _)
      .map(year => returnDateForCalendarYear(year))
      .flatMap(returnDateForYear => checkReturn(returnDateForYear, returns, today)).toList
  }

  private def checkReturn(taxYearEnd: LocalDate, returns: Seq[Return], today: LocalDate): Option[Reason] = {
    returns.find(_.taxYearEnd == taxYearEnd) match {
      //To match and return a reason for ineligibility the following needs to be true:
      //The issued date needs to be today or earlier, which means we have informed them
      //The received date needs to be after today or not exist which means we haven't received the return yet
      //If it is the most recent return which could have a due date that has not yet expired, they should be eligible and thus pass this check
      case Some(taxReturn) if isDatePresentAndAfterToday(today, taxReturn.dueDate) => None
      case Some(taxReturn) if isDateTodayOrEarlier(today, taxReturn.issuedDate) && isDateAfterTodayOrNonExistent(today, taxReturn.receivedDate) =>
        Some(ReturnNeedsSubmitting)
      case _ => None
    }
  }

  // Terminology:
  // Debt - any money owed that is 30 days or more overdue
  // Charge - any money owed that less than 30 days overdue
  // Liability - any money that is not yet due
  private def checkDebits(debits: Seq[Debit], today: LocalDate): List[Reason] = {
    val chargeStartDay: LocalDate = today.minusDays(1)
    val dateBeforeWhichDebtIsConsideredOld: LocalDate = today.minusDays(numberOfDaysAfterDueDateForDebtToBeConsideredOld)

    val (liabilities, chargesAndDebts) = debits.partition(_.dueDate.isAfter(chargeStartDay))
    val debt = chargesAndDebts.filterNot(_.dueDate.isAfter(dateBeforeWhichDebtIsConsideredOld))

    val totalLiabilities = liabilities.map(l => getTotalForDebit(l)).sum
    val totalChargesAndDebt = chargesAndDebts.map(cd => getTotalForDebit(cd)).sum
    val totalDebt = debt.map(d => getTotalForDebit(d)).sum
    val totalOwed = totalChargesAndDebt + totalLiabilities

    if (totalOwed == 0) List(NoDebt) else runDebtChecks(totalDebt, totalOwed)
  }

  private def runDebtChecks(totalDebt: Double, totalOwed: Double) =
    checkIfOldDebtIsTooHigh(totalDebt) ++ checkIfDebtIsInsignificant(totalOwed) ++ checkIfTotalDebtIsTooHigh(totalOwed)

  private def checkIfOldDebtIsTooHigh(totalDebt: Double) =
    if (totalDebt > insignificantDebtUpperLimit) List(OldDebtIsTooHigh) else Nil

  private def checkIfDebtIsInsignificant(totalOwed: Double) =
    if (totalOwed < insignificantDebtUpperLimit) List(DebtIsInsignificant) else Nil

  private def checkIfTotalDebtIsTooHigh(totalOwed: Double) =
    if (totalOwed >= maximumDebtForSelfServe) List(TotalDebtIsTooHigh) else Nil

  private def getTotalForDebit(debit: Debit) =
    (debit.interest.map(i => i.amount).getOrElse(BigDecimal(0)) + debit.amount).doubleValue()

  private def taxYearEndDateForCalendarYear(today: LocalDate) = {
    val currentCalendarYearsReturnDate = returnDateForCalendarYear(today.getYear)
    if (today.isAfter(currentCalendarYearsReturnDate)) returnDateForCalendarYear(today.getYear + 1) else currentCalendarYearsReturnDate
  }

  private def returnDateForCalendarYear(year: Int) = LocalDate.of(year, taxYearEndDay.getMonthValue, taxYearEndDay.getDayOfMonth)

  private def isDateTodayOrEarlier(today: LocalDate, dateOption: Option[LocalDate]) = dateOption match {
    case Some(date: LocalDate) => date.isBefore(today) || date.isEqual(today)
    case _                     => false
  }

  private def isDateAfterTodayOrNonExistent(today: LocalDate, maybeDate: Option[LocalDate]) = maybeDate match {
    case Some(date: LocalDate) => date.isAfter(today)
    case _                     => true
  }

  private def isDatePresentAndAfterToday(today: LocalDate, maybeDate: Option[LocalDate]) = maybeDate.exists(_.isAfter(today))
}
