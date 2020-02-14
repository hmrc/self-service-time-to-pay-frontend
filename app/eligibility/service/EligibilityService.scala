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
  val numberOfDaysAfterDueDateForDebtToBeConsideredOld = 30
  val returnHistoryYearsRequired = 4
  val taxYearEndDay: MonthDay = MonthDay.of(4, 5)

  def determineEligibility(eligibilityRequest: EligibilityRequest, onIa: Boolean): EligibilityStatus = {
    val today: LocalDate = eligibilityRequest.dateOfEligibilityCheck
    val returnsAndDebits: ReturnsAndDebits = eligibilityRequest.returnsAndDebits
    val isOnIa: List[Reason] = if (onIa) Nil else List(IsNotOnIa)

    val reasons = (checkReturnsUpToDate(returnsAndDebits.returns, today) ++ checkDebits(returnsAndDebits, today) ++ isOnIa)
    reasons match {
      case Nil => EligibilityStatus(true, Seq.empty)
      case _   => EligibilityStatus(false, reasons)
    }
  }

  def checkIssuedAndReceivedDate(issuedDate: Option[LocalDate], receivedDate: Option[LocalDate], today: LocalDate): Boolean = {
    isDateTodayOrEarlier(today, issuedDate)
    isDateAfterNow(today, receivedDate)
  }

  def isDateTodayOrEarlier(today: LocalDate, dateOption: Option[LocalDate]): Boolean = {
    dateOption match {
      case Some(date: LocalDate) =>
        if (date.isBefore(today) || date.isEqual(today)) true
        else false
      case _ => false
    }
  }

  def isDateAfterNow(today: LocalDate, dateOption: Option[LocalDate]): Boolean = {
    dateOption match {
      case Some(date: LocalDate) =>
        if (date.isAfter(today)) true
        else false
      case _ => false
    }
  }

  def checkReturnForYear(taxYearEnd: LocalDate, returns: Seq[Return], today: LocalDate): Option[Reason] = {
    returns.find(_.taxYearEnd == taxYearEnd) match {
      //case Some(r) if r.issuedDate(today) && !r.received(today) => Some(ReturnNeedsSubmitting(taxYearEnd.getYear))
      //TODO confirm this is how it should function seems a lickle bit weird
      //The issued date needs to be the same as today or earlier
      //The received date needs to be not the same as today or earlier

      case Some(aReturn) if checkIssuedAndReceivedDate(aReturn.issuedDate, aReturn.receivedDate, today) => Some(ReturnNeedsSubmitting)
      case _ => None
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
  //TODO check func
  def getTotalForDebit(debit: Debit): Double = {
    (debit.interest.map(i => i.amount).getOrElse(BigDecimal(0)) + debit.amount).doubleValue()
  }
  //TODO refactor this hell
  def checkDebits(returnsAndDebits: ReturnsAndDebits, today: LocalDate): List[Reason] = {
    val chargeStartDay: LocalDate = today.minusDays(1)
    val chargeEndDay: LocalDate = today.minusDays(numberOfDaysAfterDueDateForDebtToBeConsideredOld)
    val reasons: List[Reason] = Nil

    val (liabilities, overdue) = returnsAndDebits.debits.partition(_.dueDate.isAfter(chargeStartDay))
    val debts = overdue.filterNot(_.dueDate.isAfter(chargeEndDay))

    val totalOverdue = overdue.map(o => getTotalForDebit(o)).sum
    val totalDebt = debts.map(d => getTotalForDebit(d)).sum
    val totalEverything = totalOverdue + liabilities.map(l => getTotalForDebit(l)).sum

    if (totalEverything == 0) NoDebt :: Nil
    else {
      val reason1 = if (totalDebt > insignificantDebtUpperLimit) OldDebtIsTooHigh :: reasons else reasons
      val reason2 = if (totalEverything < insignificantDebtUpperLimit) DebtIsInsignificant :: reason1 else reason1
      val reason3 = if (totalEverything >= maximumDebtForSelfServe) TotalDebtIsTooHigh :: reason2 else reason2
      reason3
    }
  }

  private def taxYearEndDateForCalendarYear(today: LocalDate): LocalDate = {
    val currentCalendarYearsReturnDate = returnDateForCalendarYear(today.getYear)
    if (today.isAfter(currentCalendarYearsReturnDate)) returnDateForCalendarYear(today.getYear + 1) else currentCalendarYearsReturnDate
  }

  private def returnDateForCalendarYear(year: Int) = LocalDate.of(year, taxYearEndDay.getMonthValue, taxYearEndDay.getDayOfMonth)

  //________________________________________

  //  def determineEligibilityOLD(eligibilityRequest: EligibilityRequest, onIa: Boolean): EligibilityResult = {
  //    val today: LocalDate = eligibilityRequest.dateOfEligibilityCheck
  //
  //    val selfAssessmentDetails = eligibilityRequest.taxpayer.selfAssessment
  //
  //    val chargeStartDay: LocalDate = today.minusDays(1)
  //    val chargeEndDay: LocalDate = today.minusDays(numberOfDaysAfterDueDateForDebtToBeConsideredOld)
  //
  //      def checkReturnForYear(taxYearEnd: LocalDate): Option[Reason] = {
  //        selfAssessmentDetails.returns.find(_.taxYearEnd == taxYearEnd) match {
  //          //case Some(r) if r.issuedDate(today) && !r.received(today) => Some(ReturnNeedsSubmitting(taxYearEnd.getYear))
  //          //TODO confirm this is how it should function seems a lickle bit weird
  //            //The issued date needs to be the same as today or earlier
  //          //The received date needs to be not the same as today or earlier
  //
  //          case Some(r) if checkIssuedAndReceivedDate(r.issuedDate, r.receivedDate) => Some(ReturnNeedsSubmitting(taxYearEnd.getYear))
  //          case _ => None
  //        }
  //      }
  //
  //      //Validates if a user has submitted their
  //      //returns for the required period and returns any years that require returns submitting if any
  //      def checkReturnsUpToDate(): List[Reason] = {
  //        (0 to returnHistoryYearsRequired).reverse
  //          .map(currentTaxYearEndDate.getYear - _)
  //          .map(x => returnDateForCalendarYear(x))
  //          .flatMap(y => checkReturnForYear(y)).toList
  //      }
  //
  //      //Checks a user's debits whether their amounts are over 10k
  //      //or are over 30 days old and returns reasons for ineligibility if any
  //      def checkDebits(): List[Reason] = {
  //        val reasons: List[Reason] = Nil
  //
  //        val (liabilities, overdue) = selfAssessmentDetails.debits.getOrElse(Seq.empty) partition (_.dueDate.isAfter(chargeStartDay))
  //        val debts = overdue.filterNot(_.dueDate.isAfter(chargeEndDay))
  //
  //        val totalOverdue = overdue.map(_.total).sum
  //        val totalDebt = debts.map(_.total).sum
  //        val totalEverything = totalOverdue + liabilities.map(_.total).sum
  //
  //        if (totalEverything == 0) NoDebt :: Nil
  //        else {
  //          val reason1 = if (totalDebt > insignificantDebtUpperLimit) OldDebtIsTooHigh :: reasons else reasons
  //          val reason2 = if (totalEverything < insignificantDebtUpperLimit) DebtIsInsignificant :: reason1 else reason1
  //          val reason3 = if (totalEverything >= maximumDebtForSelfServe) TotalDebtIsTooHigh :: reason2 else reason2
  //          reason3
  //        }
  //      }
  //
  //    val isOnIa: List[Reason] = if (onIa) Nil else List(IsNotOnIa)
  //    val reasons = checkReturnsUpToDate() ++ checkDebits() ++ isOnIa
  //    reasons match {
  //      case Nil => Eligible
  //      case _   => Ineligible(reasons)
  //    }
  //  }
}