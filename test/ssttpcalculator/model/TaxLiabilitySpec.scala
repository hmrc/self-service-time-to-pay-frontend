/*
 * Copyright 2022 HM Revenue & Customs
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

package ssttpcalculator.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ssttpcalculator.model.TaxLiability.{amortizedLiabilities, latePayments}

import java.time.{LocalDate, Month}

class TaxLiabilitySpec extends AnyWordSpec with Matchers {

  "The TaxLiabilities module" when {
    "A payment is made" should {
      val D380_60 = BigDecimal(380.60)
      val D1779_60 = BigDecimal(1779.60)
      val D500_00 = BigDecimal(500.00)
      val JanuaryDueDate = LocalDate.of(2022, Month.JANUARY, 31)
      val JulyDueDate = LocalDate.of(2022, Month.JULY, 31)
      val FebruaryPayment = Payment(LocalDate.of(2022, Month.FEBRUARY, 28), D500_00)

      val liabilities = List(TaxLiability(D380_60, JanuaryDueDate),
                             TaxLiability(D1779_60, JanuaryDueDate),
                             TaxLiability(D1779_60, JulyDueDate))
      "remove paid up liabilities" in {
        amortizedLiabilities(liabilities, D500_00).size shouldBe 2
      }
      "reduce a liability by the remaining payment" in {
        amortizedLiabilities(liabilities, D500_00).head.amount shouldBe D1779_60 - D500_00 + D380_60
      }
      "generate the correct number of late payments on receipt of a payment" in {
        val payment = Payment(LocalDate.of(2022, Month.FEBRUARY, 28), D500_00)
        val result = latePayments(payment)(liabilities)
        result.size shouldBe 2
      }
      "generate the correct late payment values on receipt of a payment" in {
        val result = latePayments(FebruaryPayment)(liabilities)
        result.map(_.amount).sum shouldBe FebruaryPayment.amount
      }
    }
  }

}
