package ssttpcalculator.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Month
import java.time.LocalDate

class TaxPaymentPlanSpec extends AnyWordSpec with Matchers {

  "A TaxPaymentPlan" when {
    "the plan has an initial payment and has not provide an initial payment date" should {
      val liabilities = List(TaxLiability(BigDecimal(3559.20),LocalDate.of(2022,Month.JANUARY, 31)),
        TaxLiability(BigDecimal(1779.60),LocalDate.of(2022,Month.JANUARY, 31)),
        TaxLiability(BigDecimal(1779.60),LocalDate.of(2022,Month.JULY, 31)))
      val plan = TaxPaymentPlan(liabilities, BigDecimal(4000.00), LocalDate.of(2021, Month.OCTOBER, 28), LocalDate.of(2022,Month.FEBRUARY, 28))
      "remove liabilities covered by the initial payment" in {
          plan.outstandingLiabilities.size shouldBe 2
      }
      "have remaining liability with a value equal to the value of the initial liabilities less the initial payment" in {
         plan.outstandingLiabilities.map(_.amount).sum shouldBe plan.remainingLiability
      }
      "have an actual start date equal to the start date" in {
          plan.actualStartDate shouldBe plan.startDate
      }
    }
  }

}
