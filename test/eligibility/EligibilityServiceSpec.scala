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

package eligibility

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import ssttpeligibility.EligibilityService
import timetopaytaxpayer.cor.model._
import uk.gov.hmrc.selfservicetimetopay.models._

class EligibilityServiceSpec extends WordSpecLike with GuiceOneAppPerSuite with Matchers {

  val Eligible = EligibilityStatus(true, Seq.empty)
  def Ineligible(reasons: Seq[Reason]): EligibilityStatus = {
    EligibilityStatus(false, reasons)
  }
  val optionalNow = Some(LocalDate.now())
  val taxYearEnd2020 = LocalDate.of(2020, 4, 5)
  val completedReturnThisYear =
    Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 3, 6)), receivedDate = Some(LocalDate.of(2016, 2, 10)))

  "eligibility service" should {

    "grant eligibility if all returns are filed and all amounts owed are liabilities" in {
      val returns = lastFourCalendarYears.map(filedReturn)
      val debts = Seq(charge(200))

      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debts, returns)), true) shouldBe Eligible
    }

    """grant eligibility if all returns for last four years are filed but five years ago a return remains unfiled
      |and all amounts owed are liabilities""".stripMargin in {
      val returns = lastFourCalendarYears.map(filedReturn) :+ unFiledReturn(2011)
      val debts = Seq(charge(200))

      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debts, returns)), true) shouldBe Eligible
    }

    """grant eligibility if all returns are filed except the most recent which is still outstanding but not overdue
      |and all amounts owed are liabilities""".stripMargin in {
      val returns = lastThreeCalendarYears.map(filedReturn) :+ outstandingReturnThisYearButNotOverdue
      val debts = Seq(charge(200, afterTaxYearStart))

      EligibilityService.runEligibilityCheck(EligibilityRequest(afterTaxYearStart,
                                                                createTaxpayer(debts, returns)), true) shouldBe Eligible
    }

    """not grant eligibility if all returns are filed except the most recent which is still outstanding AND overdue
      |and all amounts owed are liabilities""".stripMargin in {
      val returns = lastThreeCalendarYears.map(filedReturn) :+ outstandingReturnThisYearAndOverdue
      val debts = Seq(charge(200, afterTaxYearStart))

      EligibilityService.runEligibilityCheck(EligibilityRequest(afterTaxYearStart,
                                                                createTaxpayer(debts, returns)), true) shouldBe Ineligible(List(ReturnNeedsSubmitting))
    }

    """not grant eligibility if all returns are filed except the most recent which is still outstanding and has a missing due date
      |and all amounts owed are liabilities""".stripMargin in {
      val returns = lastThreeCalendarYears.map(filedReturn) :+ outstandingReturnThisYearWithMissingDueDate
      val debts = Seq(charge(200, afterTaxYearStart))

      EligibilityService.runEligibilityCheck(EligibilityRequest(afterTaxYearStart,
                                                                createTaxpayer(debts, returns)), true) shouldBe Ineligible(List(ReturnNeedsSubmitting))
    }

    """not grant eligibility if a return in the last four years has been issued and is overdue
      |and all amounts owed are liabilities""".stripMargin in {
      val debts = Seq(charge(200))

      val returns = replaceYearWith(lastFourCalendarYears.map(filedReturn), 2013, unFiledReturn(2013))
      val result = EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debts, returns)), true)
      result shouldBe Ineligible(List(ReturnNeedsSubmitting))

      val returns2 = replaceYearWith(lastFourCalendarYears.map(filedReturn), 2015, unFiledReturn(2015))
      val result2 = EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debts, returns2)), true)
      result2 shouldBe Ineligible(List(ReturnNeedsSubmitting))
    }

    """grant eligibility if all returns issued have been filed in last four years
      |and all amounts owed are liabilities""".stripMargin in {
      val debts = Seq(charge(200))
      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debts, Nil)), true) shouldBe Eligible

      val returns2 = lastFourCalendarYears.map(unissuedReturn)
      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debts, returns2)), true) shouldBe Eligible

      val returns3 = replaceYearWith(lastFourCalendarYears.map(filedReturn), 2012, unissuedReturn(2012))
      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debts, returns3)), true) shouldBe Eligible
    }

    "not grant eligibility if this years return is overdue and all amounts owed are liabilities" in {
      val returns = lastThreeCalendarYears.map(filedReturn) :+ overdueReturnThisYear
      val debts = Seq(charge(200, afterTaxYearStart))

      EligibilityService.runEligibilityCheck(EligibilityRequest(afterTaxYearStart,
                                                                createTaxpayer(debts, returns)), true) shouldBe Ineligible(List(ReturnNeedsSubmitting))
    }

    "not grant eligibility if all returns are filed and charges total less than £32" in {
      val returns = lastFourCalendarYears.map(filedReturn)
      val singleDebtLessThan32Pounds = Seq(charge(11))
      val combinedDebtsLessThan32Pounds = Seq(charge(10), charge(21))

      val result1 = EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(singleDebtLessThan32Pounds, returns)), true)
      result1 shouldBe Ineligible(List(DebtIsInsignificant))
      val result2 = EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(combinedDebtsLessThan32Pounds, returns)), true)
      result2 shouldBe Ineligible(List(DebtIsInsignificant))
    }

    "not grant eligibility if all returns are filed and charges total over £10k" in {
      val returns = lastFourCalendarYears.map(filedReturn)
      val singleDebtOver1000Pounds = Seq(charge(10000.01))
      val combinedDebtOver1000Pounds = Seq(charge(2000), charge(9000))

      val result = EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(singleDebtOver1000Pounds, returns)), true)
      result shouldBe Ineligible(List(TotalDebtIsTooHigh))

      val result2 = EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(combinedDebtOver1000Pounds, returns)), true)
      result2 shouldBe Ineligible(List(TotalDebtIsTooHigh))
    }

    "not grant eligibility if all returns are filed and no debts are owed" in {
      val returns = lastFourCalendarYears.map(filedReturn)

      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(Nil, returns)), true) shouldBe Ineligible(List(NoDebt))
    }

    "not grant eligibility if all returns are filed and debt over 59 days is over £32" in {
      val returns = lastFourCalendarYears.map(filedReturn)
      val debts = Seq(debt(32.01))

      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart,
                                                                createTaxpayer(debts, returns)), true) shouldBe Ineligible(List(OldDebtIsTooHigh))
    }

    "grant eligibility if all returns are filed and debt over 29 days is £32 and charges or liabilities are £9967.99" in {
      val returns = lastFourCalendarYears.map(filedReturn)
      val debitsWithCharge = Seq(debt(32.00), charge(9967.99))
      val debitsWithLiabilities = Seq(debt(32.00), liability(9967.99))

      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debitsWithCharge, returns)), true) shouldBe Eligible
      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart, createTaxpayer(debitsWithLiabilities, returns)), true) shouldBe Eligible
    }

    "not grant eligibility if all returns are filed and debt over 59 days is £32.01 and charges or liabilities are £9967.99" in {
      val returns = lastFourCalendarYears.map(filedReturn)
      val debitsWithCharge = Seq(debt(32.01), charge(9967.99))
      val debitsWithLiabilities = Seq(debt(32.01), liability(9967.99))

      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart,
                                                                createTaxpayer(debitsWithCharge, returns)), onIa = true) shouldBe Ineligible(List(OldDebtIsTooHigh, TotalDebtIsTooHigh))
      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart,
                                                                createTaxpayer(debitsWithLiabilities, returns)), true) shouldBe Ineligible(List(OldDebtIsTooHigh, TotalDebtIsTooHigh))
    }

    "not grant eligibility if all returns are filed and liabilities total over £10k" in {
      val returns = lastFourCalendarYears.map(filedReturn)
      val debits = Seq(liability(10000.01))

      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(TotalDebtIsTooHigh))
    }

    "consider interest as debt for its rules" in {
      val returns = lastFourCalendarYears.map(filedReturn)
      val debitsOver10k = Seq(charge(9000, interest = Some(Interest(optionalNow, 1000.01))))
      val debitsUnder10k = Seq(charge(9000, interest = Some(Interest(optionalNow, 999.99))))
      val debits10k = Seq(charge(9000, interest = Some(Interest(optionalNow, 1000))))

      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart,
                                                                createTaxpayer(debitsOver10k, returns)), true) shouldBe Ineligible(List(TotalDebtIsTooHigh))
      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart,
                                                                createTaxpayer(debits10k, returns)), true) shouldBe Ineligible(List(TotalDebtIsTooHigh))
      EligibilityService.runEligibilityCheck(EligibilityRequest(beforeTaxYearStart,
                                                                createTaxpayer(debitsUnder10k, returns)), true) shouldBe Eligible
    }

    "SA Return not yet submitted by customer" in {
      val debits = List(
        Debit(amount     = 1000, dueDate = LocalDate.of(2017, 1, 31),
              interest   = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 1, 31),
              interest   = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 7, 31),
              interest   = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5),
                           issuedDate   = Some(LocalDate.of(2015, 4, 5)), receivedDate = Some(LocalDate.of(2016, 2, 1))) :: Nil

      val todaysDate = LocalDate.of(2016, 1, 31)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(ReturnNeedsSubmitting))
    }

    "SA Return submitted in future by customer" in {
      val debits = List(
        Debit(amount     = 1000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 2, 1))) :: Nil

      val todaysDate = LocalDate.of(2016, 1, 31)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(ReturnNeedsSubmitting))
    }

    "Return submitted, total amount > £10k" in {
      val debits = List(
        Debit(amount     = 5000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 10000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 10000, dueDate = LocalDate.of(2017, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 12, 10))) :: Nil

      val todaysDate = LocalDate.of(2016, 12, 31)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(TotalDebtIsTooHigh))
    }

    "Return submitted, debt > £32 is older than 29 days" in {
      val debits = List(
        Debit(amount     = 2000, dueDate = LocalDate.of(2016, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 1000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020))

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 12, 10))) :: Nil

      val todaysDate = LocalDate.of(2016, 12, 31)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(OldDebtIsTooHigh))
    }

    "Return submitted, old debt < £32, total amount < £10k" in {
      val debits = List(
        Debit(amount     = 0, dueDate = LocalDate.of(2016, 6, 10), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 1000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 12, 10))) :: Nil

      val todaysDate = LocalDate.of(2016, 12, 31)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Eligible
    }

    "Return submitted, old returns overdue" in {
      val debits = List(
        Debit(amount     = 1000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = List(
        Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
               receivedDate = Some(LocalDate.of(2016, 12, 10))),
        Return(taxYearEnd   = LocalDate.of(2015, 4, 5), issuedDate = Some(LocalDate.of(2014, 4, 5)),
               receivedDate = None))

      val todaysDate = LocalDate.of(2016, 12, 31)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(ReturnNeedsSubmitting))
    }

    "Return submitted, total amount <£10k" in {
      val debits = List(
        Debit(amount     = 1000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 12, 10))) :: Nil

      val todaysDate = LocalDate.of(2017, 2, 4)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Eligible
    }

    "Return submitted, total amount >£10k with future liability" in {
      val debits = List(
        Debit(amount     = 3000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 6000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 6000, dueDate = LocalDate.of(2017, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 12, 10))) :: Nil

      val todaysDate = LocalDate.of(2017, 2, 4)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(TotalDebtIsTooHigh))
    }

    "Return submitted, old debts > £32" in {
      val debits = List(
        Debit(amount     = 1000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 2000, dueDate = LocalDate.of(2017, 7, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 0, dueDate = LocalDate.of(2016, 1, 31), interest = Some(Interest(Some(LocalDate.now), 25)), originCode = "IN1", taxYearEnd = taxYearEnd2020),
        Debit(amount     = 0, dueDate = LocalDate.of(2016, 1, 31), interest = Some(Interest(Some(LocalDate.now), 40)), originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 12, 10))) :: Nil

      val todaysDate = LocalDate.of(2016, 12, 31)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(OldDebtIsTooHigh))
    }

    "Return submitted, total liability > £32" in {
      val debits = List(
        Debit(amount     = 30, dueDate = LocalDate.of(2017, 7, 31), interest = None, originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 12, 10))) :: Nil

      val todaysDate = LocalDate.of(2017, 7, 10)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), true) shouldBe Ineligible(List(DebtIsInsignificant))
    }

    "Return submitted, utr not on ia " in {
      val debits = List(
        Debit(amount     = 500, dueDate = LocalDate.of(2017, 7, 31), interest = None, originCode = "IN1", taxYearEnd = taxYearEnd2020)
      )

      val returns = Return(taxYearEnd   = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 4, 5)),
                           receivedDate = Some(LocalDate.of(2016, 12, 10))) :: Nil

      val todaysDate = LocalDate.of(2017, 7, 10)

      EligibilityService.runEligibilityCheck(EligibilityRequest(todaysDate,
                                                                createTaxpayer(debits, returns)), false) shouldBe Ineligible(List(IsNotOnIa))
    }
  }

  val outstandingReturnThisYearButNotOverdue = Return(taxYearEnd = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 3, 6)), dueDate = Some(LocalDate.of(2017, 7, 11)))
  val outstandingReturnThisYearAndOverdue = Return(taxYearEnd = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 3, 6)), dueDate = Some(LocalDate.of(2016, 2, 1)))
  val outstandingReturnThisYearWithMissingDueDate = Return(taxYearEnd = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 3, 6)), dueDate = None)
  val overdueReturnThisYear = Return(taxYearEnd = LocalDate.of(2016, 4, 5), issuedDate = Some(LocalDate.of(2015, 3, 6)))
  val lastThreeCalendarYears: Seq[Int] = Seq(2013, 2014, 2015)
  val lastFourCalendarYears: Seq[Int] = Seq(2012, 2013, 2014, 2015)

  def liability(amount: Double, currentDate: LocalDate = beforeTaxYearStart, interest: Option[Interest] = None) =
    debit(amount, currentDate.plusDays(1), interest = interest)

  def charge(amount: Double, currentDate: LocalDate = beforeTaxYearStart, interest: Option[Interest] = None) =
    debit(amount, currentDate.minusDays(29), interest = interest)

  def debt(amount: Double, currentDate: LocalDate = beforeTaxYearStart, interest: Option[Interest] = None) =
    debit(amount, currentDate.minusDays(60), interest = interest)

  def debit(amount: Double, dueDate: LocalDate, interest: Option[Interest] = None) = {
    Debit(amount     = amount, dueDate = dueDate, interest = interest, originCode = "IN1", taxYearEnd = taxYearEnd2020)
  }

  def filedReturn(year: Int) = Return(taxYearEnd   = LocalDate.of(year, 4, 5), issuedDate = Some(LocalDate.of(year - 1, 3, 6)),
                                      receivedDate = Some(LocalDate.of(year, 12, 10)))

  def unissuedReturn(year: Int) = Return(taxYearEnd = LocalDate.of(year, 4, 5))

  def unFiledReturn(year: Int) = Return(taxYearEnd = LocalDate.of(year, 4, 5), issuedDate = Some(LocalDate.of(year - 1, 3, 6)))

  def beforeTaxYearStart = LocalDate.of(2016, 2, 2)

  def afterTaxYearStart = LocalDate.of(2016, 9, 2)

  def replaceYearWith(returns: Seq[Return], year: Int, `return`: Return): Seq[Return] = returns.map(r => if (r.taxYearEnd.getYear == year) `return` else r)

  def address(): Address = {
    Address(Some("1 Donut Street"), Some("Balaxian Waystation"), Some("Algaranius Cluster"), Some("Becolian Sector"), Some("Andromeda Galaxy"), Some("BN1 1AA"))
  }

  private def createTaxpayer(debits: Seq[Debit], returns: Seq[Return]) = {
    Taxpayer(selfAssessment = SelfAssessmentDetails(debits                   = debits, returns = returns, communicationPreferences = CommunicationPreferences(true, true, true, true), utr = SaUtr("6573196998")), customerName = "Mr Eric Biddle", addresses = Seq(address))
  }
}
