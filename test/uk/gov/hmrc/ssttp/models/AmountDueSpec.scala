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

package uk.gov.hmrc.ssttp.models

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import java.time.LocalDate
import java.math.BigDecimal

class AmountDueSpec extends UnitSpec with MockitoSugar {

  "AmountDue" should {

    "create with year month date parameters" in {
      val amountDue = new CalculatorAmountDue(new BigDecimal("1000.00"), 2016, 10, 26)
      amountDue shouldNot be (null)
      amountDue.amount.compare(new BigDecimal(1000.00)) should be (0)
      amountDue.dueByYear should be (2016)
      amountDue.dueByMonth should be (10)
      amountDue.dueByDay should be (26)
    }

    "create with LocalDate parameter" in {
      val date = LocalDate.of(2016, 10, 26)
      val amountDue = new CalculatorAmountDue(new BigDecimal("1000.00"), date)
      amountDue shouldNot be (null)
      amountDue.amount.compare(new BigDecimal(1000.00)) should be (0)
      amountDue.dueByYear should be (2016)
      amountDue.dueByMonth should be (10)
      amountDue.dueByDay should be (26)
    }

    "return correct DueByDate in getDueBy" in {
      val date = LocalDate.of(2016, 10, 26)
      val amountDue = new CalculatorAmountDue(new BigDecimal("1000.00"), date)
      amountDue shouldNot be (null)
      val dateReturned  = amountDue.getDueBy
      dateReturned should be(date)
    }
  }
}
