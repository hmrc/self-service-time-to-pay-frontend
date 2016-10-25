/*
 * Copyright 2016 HM Revenue & Customs
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

package models

import java.math.BigDecimal
import java.time.LocalDate

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class CalculatorInputSpec extends UnitSpec with MockitoSugar {

  "CalculatorInput" should {

    "create paymentFrequency defaulted to MONTHLY" in {
      val liability1 = mock[CalculatorLiability]
      val liability2 = mock[CalculatorLiability]
      val liabilities = Seq(liability1, liability2)
      val calculatorInput = CalculatorInput(liabilities, new BigDecimal("200.00"), LocalDate.of(2000, 1, 29), LocalDate.of(2010, 12, 29))
      calculatorInput shouldNot be (null)
      calculatorInput.paymentFrequency should be ("MONTHLY")
    }

  }
}
