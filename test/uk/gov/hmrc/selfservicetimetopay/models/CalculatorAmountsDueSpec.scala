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

package uk.gov.hmrc.selfservicetimetopay.models

import java.math.BigDecimal
import java.time.LocalDate

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class CalculatorAmountsDueSpec extends UnitSpec with MockitoSugar {

  "AmountsDue" should {
    "create with one mock amount in a seq" in {
      val cad = CalculatorAmountsDue(Seq(mock[CalculatorAmountDue]))
      cad shouldNot be (null)
      cad.amountsDue.length should be(1)
    }

    "create with two mock amounts in a seq" in {
      val cad = CalculatorAmountsDue(Seq(mock[CalculatorAmountDue], mock[CalculatorAmountDue]))
      cad shouldNot be (null)
      cad.amountsDue.length should be(2)
    }


    "correctly sum two amounts in a seq" in {
      val amounts = Seq(
          new CalculatorAmountDue(amount = BigDecimal("100.11"), dueBy = LocalDate.now()),
          new CalculatorAmountDue(BigDecimal("199.89"), LocalDate.now)
      )
      val cad = CalculatorAmountsDue(Seq(mock[CalculatorAmountDue], mock[CalculatorAmountDue]))
      cad.total.compare(BigDecimal("300.00")) should be (0)
      cad.amountsDue.length should be(2)
    }


  }
}
