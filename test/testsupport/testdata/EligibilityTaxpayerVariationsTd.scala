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

package testsupport.testdata

import java.time.LocalDate

import timetopaytaxpayer.cor.model._
import uk.gov.hmrc.selfservicetimetopay.models._

object EligibilityTaxpayerVariationsTd {
  private val dummyCurrentDate = LocalDate.of(2019, 11, 1)
  private val dummyTaxYearEnd = LocalDate.of(2020, 4, 5)
  private val dummy60DaysAgo = dummyCurrentDate.minusDays(60)
  private val eligibleDebits = Seq(debit(33, dummyCurrentDate))

  private def debit(amount: Double, dueDate: LocalDate) =
    Debit("IN1", BigDecimal(amount), dueDate, Some(Interest(Some(dummyCurrentDate), BigDecimal(0))), dummyTaxYearEnd)

  private def returnsAndDebits(debits: Seq[Debit] = Seq.empty, returns: Seq[Return] = Seq.empty) =
    ReturnsAndDebits(debits, returns)

  def getIneligibleTaxpayerModel(reason: Reason): ReturnsAndDebits = {
    reason match {
      case NoDebt              => returnsAndDebits(Seq.empty)
      case DebtIsInsignificant => returnsAndDebits(Seq(debit(31, dummyCurrentDate)))
      case OldDebtIsTooHigh    => returnsAndDebits(Seq(debit(33, dummy60DaysAgo)))
      case TotalDebtIsTooHigh  => returnsAndDebits(Seq(debit(10000, dummyCurrentDate)))
      case ReturnNeedsSubmitting =>
        returnsAndDebits(eligibleDebits, Seq(Return(dummyTaxYearEnd, Some(dummy60DaysAgo), Some(dummyCurrentDate), None)))
      case IsNotOnIa => returnsAndDebits(eligibleDebits)
      case _         => TdAll.returnsAndDebits
    }
  }
}
