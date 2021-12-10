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

import java.time.LocalDate
import play.api.libs.json.{Json, JsonValidationError, OFormat, OWrites}

import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode.HALF_UP

case class TaxPaymentPlan(
    liabilities:      Seq[TaxLiability],
    initialPayment:   BigDecimal,
    startDate:        LocalDate,
    endDate:          LocalDate,
    firstPaymentDate: Option[LocalDate] = None
) {

  import TaxPaymentPlan._

  def totalLiability: BigDecimal = liabilities.map(_.amount).sum - initialPayment

  def monthlyRepayment(months: Int): BigDecimal = (totalLiability / months).setScale(2, HALF_UP)

  def actualStartDate = firstPaymentDate.getOrElse(startDate)

  def applyInitialPayment: Seq[TaxLiability] = {
    val result = liabilities.sortBy(_.dueDate).foldLeft((initialPayment, Seq.empty[TaxLiability])){
      case ((p, s), lt) if p == 0         => (p, s :+ lt.copy(dueDate = if (startDate.isBefore(lt.dueDate)) lt.dueDate else startDate))

      case ((p, s), lt) if p >= lt.amount => (p - lt.amount, s)

      case ((p, s), lt) if p < lt.amount => (0, s :+ lt.copy(amount  = lt.amount - p,
                                                             dueDate = if (startDate.plusWeeks(1).isBefore(lt.dueDate)) lt.dueDate else startDate.plusWeeks(1)))
    }
    result._2
  }

}

object TaxPaymentPlan {

  private val reads = Json.reads[TaxPaymentPlan]
    .filter(JsonValidationError("'debits' was empty, it should have at least one debit."))(_.liabilities.size > 0)
    .filter(JsonValidationError("The 'initialPayment' can't be less than 0"))(_.initialPayment >= 0)

  private val writes: OWrites[TaxPaymentPlan] = Json.writes[TaxPaymentPlan]

  implicit val format: OFormat[TaxPaymentPlan] = OFormat(reads, writes)

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

}
