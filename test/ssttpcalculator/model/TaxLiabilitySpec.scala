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

package ssttpcalculator.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ssttpcalculator.model.TaxLiability.amortizedLiabilities

import java.time.{LocalDate, Month}

class TaxLiabilitySpec extends AnyWordSpec with Matchers {

  "The TaxLiabilities module" when {
    "A payment is made" should {
      val liabilities = List(TaxLiability(BigDecimal(380.60), LocalDate.of(2022, Month.JANUARY, 31)),
                             TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JULY, 31)))
      "remove paid up liabilities" in {
        amortizedLiabilities(liabilities, BigDecimal(500)).size shouldBe 1
      }
      "reduce a liability by the remaining payment" in {
        amortizedLiabilities(liabilities, BigDecimal(500)).head.amount shouldBe BigDecimal(1779.60) - BigDecimal(500) + BigDecimal(380.60)
      }
    }
  }

}
