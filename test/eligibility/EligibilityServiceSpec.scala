/*
 * Copyright 2021 HM Revenue & Customs
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

package eligibility

import java.time.LocalDate

import ssttpeligibility.EligibilityService
import testsupport.{DateSupport, ItSpec}
import timetopaytaxpayer.cor.model._
import uk.gov.hmrc.selfservicetimetopay.models.EligibilityStatus.Eligible
import uk.gov.hmrc.selfservicetimetopay.models.{DirectDebitInstruction, _}

class EligibilityServiceSpec extends ItSpec with DateSupport {
  private val today = LocalDate.parse("2020-03-30")
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)
  private val aYearAgo = today.minusYears(1)
  private val almostAYearAgo = aYearAgo.plusDays(1)

  private val origin = "IN1"

  private val onePence = 0.01
  private val debtLimit = 30000
  private val significantDebtAmount = 32.00
  private val insignificantDebtAmount = significantDebtAmount - onePence

  private val numberOfDaysBeforeADebtIsOld = 60
  private val oldDebtDate = today.minusDays(numberOfDaysBeforeADebtIsOld)

  private val last4TaxYears: Seq[Int] = Seq(_2016, _2017, _2018, _2019)
  private val filedReturns = last4TaxYears.map(filedReturn)
  private val missingReturns = Seq(filedReturns.head.copy(receivedDate = None)) ++ filedReturns.tail
  private val filedNotDueReturn = filedReturn(_2020).copy(dueDate      = Some(tomorrow), receivedDate = Some(today))
  private val overdueReturn = filedReturn(_2020).copy(dueDate      = Some(yesterday), receivedDate = None)

  private val eligibleDebit = debit(significantDebtAmount)

  private val eligibleDirectDebitInstruction = DirectDebitInstruction(
    sortCode      = Some("sortCode"),
    accountNumber = Some("accountNumber"),
    accountName   = Some("accountName"),
    creationDate  = Some(aYearAgo))

  private val directDebitCreatedWithinTheLastYear = eligibleDirectDebitInstruction.copy(creationDate = Some(almostAYearAgo))

  private val acceptableOldDebt = eligibleDebit.copy(amount  = significantDebtAmount, dueDate = oldDebtDate)
  private val oldDebtThatIsTooHigh = acceptableOldDebt.copy(amount = significantDebtAmount + onePence)
  private val noDebts = Seq.empty[Debit]

  private lazy val eligibilityService: EligibilityService = app.injector.instanceOf[EligibilityService]

  "checkEligibility should" should {
    "return eligible when" should {
      "all criteria are met" in {
        eligibility() mustBe Eligible
      }

      "the user has no direct debits created within the last year" in {
        eligibility(directDebits = directDebitInstructions(Seq.empty)) mustBe Eligible

        eligibility(directDebits =
          directDebitInstructions(Seq(
            eligibleDirectDebitInstruction,
            eligibleDirectDebitInstruction.copy(sortCode = Some("sortCode2"))))) mustBe Eligible
      }

      "the user has significant debt" in {
        eligibility(debits = Seq(debit(significantDebtAmount))) mustBe Eligible
        eligibility(debits = Seq(debit(insignificantDebtAmount), debit(onePence))) mustBe Eligible
      }

      "an otherwise eligible user has old debt which is not too high" in {
        eligibility(debits = Seq(acceptableOldDebt)) mustBe Eligible
      }

      "the user has debt which will become too high when it is older" in {
        eligibility(
          debits = Seq(oldDebtThatIsTooHigh.copy(dueDate = oldDebtDate.plusDays(1)))) mustBe Eligible
      }

      "the user has total debt which is not too high" in {
        eligibility(debits = Seq(debit(amount = debtLimit))) mustBe Eligible
        eligibility(debits = Seq(debit(amount = debtLimit - onePence), debit(amount = onePence))) mustBe Eligible
        eligibility(debits = Seq(debitWithInterest(amount   = debtLimit - onePence, interest = onePence))) mustBe Eligible
      }

      "the user has filed all due of filed tax returns" in {
        eligibility(returns = filedReturns) mustBe Eligible
        eligibility(returns = Seq(filedReturns.head)) mustBe Eligible
      }

      "the user has filed tax returns which are not yet due" in {
        eligibility(returns = filedReturns ++ Seq(filedNotDueReturn)) mustBe Eligible
      }
    }

    "return ineligible when" should {
      "an otherwise eligible user is not on IA" in {
        eligibility(onIa = false) mustBe EligibilityStatus(Seq(IsNotOnIa))
      }

      "an otherwise eligible user has direct debits created within the last year" in {
        val ineligible = EligibilityStatus(Seq(DirectDebitCreatedWithinTheLastYear))

        eligibility(directDebits = directDebitInstructions(Seq(directDebitCreatedWithinTheLastYear))) mustBe ineligible

        eligibility(directDebits =
          directDebitInstructions(Seq(eligibleDirectDebitInstruction, directDebitCreatedWithinTheLastYear))) mustBe ineligible

        eligibility(directDebits =
          directDebitInstructions(Seq(
            directDebitCreatedWithinTheLastYear,
            directDebitCreatedWithinTheLastYear.copy(sortCode = Some("sortCode2"))))) mustBe ineligible
      }

      "an otherwise eligible user has no debt" in {
        eligibility(debits = noDebts) mustBe EligibilityStatus(Seq(NoDebt))
      }

      "an otherwise eligible user has insignificant debt" in {
        val debtIsInsignificant = EligibilityStatus(Seq(DebtIsInsignificant))

        eligibility(debits = Seq(debit(insignificantDebtAmount))) mustBe debtIsInsignificant
        eligibility(debits = Seq(debitWithInterest(insignificantDebtAmount - onePence, onePence))) mustBe debtIsInsignificant
        eligibility(debits = Seq(debit(insignificantDebtAmount - onePence), debit(onePence))) mustBe debtIsInsignificant
      }

      "an otherwise eligible user has old debt which is too high" in {
        eligibility(debits = Seq(oldDebtThatIsTooHigh)) mustBe EligibilityStatus(Seq(OldDebtIsTooHigh))
      }

      "an otherwise eligible user has a debt that is too high" in
        {
          val totalDebtTooHigh: EligibilityStatus = EligibilityStatus(Seq(TotalDebtIsTooHigh))

          eligibility(debits = Seq(debit(amount = debtLimit + 1))) mustBe totalDebtTooHigh
          eligibility(debits = Seq(debitWithInterest(amount   = debtLimit, interest = onePence))) mustBe totalDebtTooHigh
          eligibility(debits = Seq(debit(amount = debtLimit), debit(amount = onePence))) mustBe totalDebtTooHigh
        }

      "an otherwise eligible user has tax returns that are overdue" in {
        eligibility(returns = missingReturns) mustBe EligibilityStatus(Seq(ReturnNeedsSubmitting))
        eligibility(returns = filedReturns ++ Seq(overdueReturn)) mustBe EligibilityStatus(Seq(ReturnNeedsSubmitting))
      }
    }

    "return all ineligibility reasons" in {
      eligibility(
        debits       = noDebts,
        returns      = missingReturns,
        directDebits = directDebitInstructions(Seq(directDebitCreatedWithinTheLastYear)),
        onIa         = false
      ).reasons.toSet mustBe Set(ReturnNeedsSubmitting, NoDebt, IsNotOnIa, DirectDebitCreatedWithinTheLastYear)
    }
  }

  private def eligibility(debits:       Seq[Debit]              = Seq(eligibleDebit),
                          returns:      Seq[Return]             = filedReturns,
                          directDebits: DirectDebitInstructions = directDebitInstructions(Seq(eligibleDirectDebitInstruction)),
                          onIa:         Boolean                 = true) = {
    eligibilityService.checkEligibility(today, taxpayer(debits, returns), directDebits, onIa)
  }

  private def debit(amount: Double) = Debit(
    amount     = amount,
    dueDate    = secondSAPaymentDate(_2020),
    interest   = Some(Interest(Some(yesterday), 0)),
    originCode = origin,
    taxYearEnd = taxYearEnd(_2020))

  private def debitWithInterest(amount: Double, interest: Double) = Debit(
    amount     = amount,
    dueDate    = secondSAPaymentDate(_2020),
    interest   = Some(Interest(Some(yesterday), interest)),
    originCode = origin,
    taxYearEnd = taxYearEnd(_2020))

  private def directDebitInstructions(directDebitInstructions: Seq[DirectDebitInstruction]) =
    DirectDebitInstructions(directDebitInstructions)

  private def filedReturnIssueDate(year: Int) = Some(LocalDate.of(year - 1, march, _6th))

  private def filedReturn(year: Int) = Return(
    taxYearEnd   = taxYearEnd(year),
    issuedDate   = filedReturnIssueDate(year),
    receivedDate = Some(LocalDate.of(year, december, _10th)))

  private def secondSAPaymentDate(year: Int) = LocalDate.of(year, july, _31st)

  private def taxpayer(debits: Seq[Debit], returns: Seq[Return]) =
    Taxpayer(
      selfAssessment =
        SelfAssessmentDetails(
          debits                   = debits,
          returns                  = returns,
          communicationPreferences =
            CommunicationPreferences(
              welshLanguageIndicator = true, audioIndicator = true, largePrintIndicator = true, brailleIndicator = true),
          utr                      = SaUtr("6573196998")),
      customerName   = "Mr Eric Biddle",
      addresses      = Seq(Address(Some("1 Donut Street"), Some("BN1 1AA"))))
}
