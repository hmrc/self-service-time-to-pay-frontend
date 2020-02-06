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
import eligibility.model._
import javax.inject.Singleton

/**
 * Determines if a tax payer is eligible for self service.
 *
 * Terminology:
 * Debt - any money owed that is 30 days or more overdue
 * Charge - any money owed that less than 30 days overdue
 * Liability - any money that is not yet due
 */
@Singleton
trait EligibilityService {

  val insignificantDebtUpperLimit = 32
  val maximumDebtForSelfServe = 10000
  val numberOfDaysAfterDueDateForDebtToBeConsideredOld = 30
  val returnHistoryYearsRequired = 4
  val taxYearEndDay: MonthDay = MonthDay.of(4, 5)

  def determineEligibility(eligibilityRequest: EligibilityRequest, onIa: Boolean): EligibilityResult = {
    val today: LocalDate = eligibilityRequest.dateOfEligibilityCheck
    val currentTaxYearEndDate: LocalDate = taxYearEndDateForCalendarYear(today)
    val saDetails = eligibilityRequest.taxpayer.selfAssessment.getOrElse(SelfAssessment(debits  = None, returns = None))

    val chargeStartDay: LocalDate = today.minusDays(1)
    val chargeEndDay: LocalDate = today.minusDays(numberOfDaysAfterDueDateForDebtToBeConsideredOld)

      def checkReturnForYear(taxYearEnd: LocalDate): Option[Reason] = {
        saDetails.returns.getOrElse(Seq.empty).find(_.taxYearEnd == taxYearEnd) match {
          case Some(r) if r.issued(today) && !r.received(today) => Some(ReturnNeedsSubmitting(taxYearEnd.getYear))
          case _ => None
        }
      }

      //Validates if a user has submitted their
      //returns for the required period and returns any years that require returns submitting if any
      def checkReturnsUpToDate(): List[Reason] = {
        (0 to returnHistoryYearsRequired).reverse
          .map(currentTaxYearEndDate.getYear - _)
          .map(returnDateForCalendarYear)
          .flatMap(checkReturnForYear).toList
      }

      //Checks a user's debits whether their amounts are over 10k
      //or are over 30 days old and returns reasons for ineligibility if any
      def checkDebits(): List[Reason] = {
        val reasons: List[Reason] = Nil

        val (liabilities, overdue) = saDetails.debits.getOrElse(Seq.empty) partition (_.dueDate.isAfter(chargeStartDay))
        val debts = overdue.filterNot(_.dueDate.isAfter(chargeEndDay))

        val totalOverdue = overdue.map(_.total).sum
        val totalDebt = debts.map(_.total).sum
        val totalEverything = totalOverdue + liabilities.map(_.total).sum

        if (totalEverything == 0) NoDebt :: Nil
        else {
          val reason1 = if (totalDebt > insignificantDebtUpperLimit) OldDebtIsTooHigh :: reasons else reasons
          val reason2 = if (totalEverything < insignificantDebtUpperLimit) DebtIsInsignificant :: reason1 else reason1
          val reason3 = if (totalEverything >= maximumDebtForSelfServe) TotalDebtIsTooHigh :: reason2 else reason2
          reason3
        }
      }

    val isOnIa: List[Reason] = if (onIa) Nil else List(IsNotOnIa)
    val reasons = checkReturnsUpToDate() ++ checkDebits() ++ isOnIa
    reasons match {
      case Nil => Eligible
      case _   => Ineligible(reasons)
    }
  }

  private def taxYearEndDateForCalendarYear(today: LocalDate): LocalDate = {
    val currentCalendarYearsReturnDate = returnDateForCalendarYear(today.getYear)
    if (today.isAfter(currentCalendarYearsReturnDate)) returnDateForCalendarYear(today.getYear + 1) else currentCalendarYearsReturnDate
  }

  private def returnDateForCalendarYear(year: Int) = LocalDate.of(year, taxYearEndDay.getMonthValue, taxYearEndDay.getDayOfMonth)

}
