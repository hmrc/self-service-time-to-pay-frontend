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

package eligibility.service

import java.time.{LocalDate, MonthDay}

import timetopaytaxpayer.cor.model.{Debit, Return, ReturnsAndDebits}
import uk.gov.hmrc.selfservicetimetopay.models.{DebtIsInsignificant, EligibilityRequest, EligibilityStatus, IsNotOnIa, NoDebt, OldDebtIsTooHigh, Reason, ReturnNeedsSubmitting, TotalDebtIsTooHigh}

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

  def determineEligibility(eligibilityRequest: EligibilityRequest, onIa: Boolean): EligibilityStatus = {
    val returnsAndDebits: ReturnsAndDebits = eligibilityRequest.returnsAndDebits
    val isOnIa: List[Reason] = if (onIa) Nil else List(IsNotOnIa)

    val reasons = (checkReturnsUpToDate(returnsAndDebits.returns, eligibilityRequest.dateOfEligibilityCheck)
      ++ checkDebits(returnsAndDebits.debits, eligibilityRequest.dateOfEligibilityCheck) ++ isOnIa)
    reasons match {
      case Nil => EligibilityStatus(true, Seq.empty)
      case _   => EligibilityStatus(false, reasons.map(r => r.name))
    }
  }

  //Validates if a user has submitted their
  //returns for the required period and returns any years that require returns submitting if any
  def checkReturnsUpToDate(returns: Seq[Return], today: LocalDate): List[Reason] = {
    val currentTaxYearEndDate: LocalDate = taxYearEndDateForCalendarYear(today)

    (0 to returnHistoryYearsRequired).reverse
      .map(currentTaxYearEndDate.getYear - _)
      .map(x => returnDateForCalendarYear(x))
      .flatMap(y => checkReturnForYear(y, returns, today)).toList
  }

  def checkReturnForYear(taxYearEnd: LocalDate, returns: Seq[Return], today: LocalDate): Option[Reason] = {
    returns.find(_.taxYearEnd == taxYearEnd) match {
      //The issued date needs to be the same as today or earlier
      //The received date needs to be not the same as today or earlier
      case Some(aReturn) if checkIssuedAndReceivedDate(aReturn.issuedDate, aReturn.receivedDate, today) => Some(ReturnNeedsSubmitting)
      case _ => None
    }
  }

  def checkIssuedAndReceivedDate(issuedDate: Option[LocalDate], receivedDate: Option[LocalDate], today: LocalDate): Boolean = {
    isDateTodayOrEarlier(today, issuedDate) && isDateAfterNow(today, receivedDate)
  }

  // Terminology:
  // Debt - any money owed that is 30 days or more overdue
  // Charge - any money owed that less than 30 days overdue
  // Liability - any money that is not yet due
  def checkDebits(debits: Seq[Debit], today: LocalDate): List[Reason] = {
    val chargeStartDay: LocalDate = today.minusDays(1)
    val dateBeforeWhichDebtIsConsideredOld: LocalDate = today.minusDays(numberOfDaysAfterDueDateForDebtToBeConsideredOld)

    val (liabilities, chargesAndDebts) = debits.partition(_.dueDate.isAfter(chargeStartDay))
    val debt = chargesAndDebts.filterNot(_.dueDate.isAfter(dateBeforeWhichDebtIsConsideredOld))
    val totalLiabilities = liabilities.map(l => getTotalForDebit(l)).sum
    val totalChargesAndDebt = chargesAndDebts.map(d => getTotalForDebit(d)).sum
    val totalDebt = debt.map(d => getTotalForDebit(d)).sum
    val totalOwed = totalChargesAndDebt + totalLiabilities

    val reasons: List[Reason] = List.empty

    if (totalOwed == 0) NoDebt :: reasons
    else {
      if (totalDebt > insignificantDebtUpperLimit) OldDebtIsTooHigh :: reasons
      if (totalOwed < insignificantDebtUpperLimit) DebtIsInsignificant :: reasons
      if (totalOwed >= maximumDebtForSelfServe) TotalDebtIsTooHigh :: reasons
      reasons
    }
  }

  def getTotalForDebit(debit: Debit): Double = {
    (debit.interest.map(i => i.amount).getOrElse(BigDecimal(0)) + debit.amount).doubleValue()
  }

  private def taxYearEndDateForCalendarYear(today: LocalDate): LocalDate = {
    val currentCalendarYearsReturnDate = returnDateForCalendarYear(today.getYear)
    if (today.isAfter(currentCalendarYearsReturnDate)) returnDateForCalendarYear(today.getYear + 1) else currentCalendarYearsReturnDate
  }

  private def returnDateForCalendarYear(year: Int) = LocalDate.of(year, taxYearEndDay.getMonthValue, taxYearEndDay.getDayOfMonth)

  def isDateTodayOrEarlier(today: LocalDate, dateOption: Option[LocalDate]): Boolean = {
    dateOption match {
      case Some(date: LocalDate) =>
        if (date.isBefore(today) || date.isEqual(today)) true
        else false
      case _ => throw new RuntimeException("Date Missing For Eligibility Check")
    }
  }

  def isDateAfterNow(today: LocalDate, dateOption: Option[LocalDate]): Boolean = {
    dateOption match {
      case Some(date: LocalDate) =>
        if (date.isAfter(today)) true
        else false
      case _ => throw new RuntimeException("Date Missing For Eligibility Check")
    }
  }
}
