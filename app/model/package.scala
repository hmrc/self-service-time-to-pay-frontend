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

import java.time.LocalDate

import timetopaytaxpayer.cor.model.Debit
import timetopaycalculator.cor.model.{DebitInput, PaymentSchedule}
import uk.gov.hmrc.domain.SaUtr

package object model {

  def asDebitInput(debit: Debit): DebitInput = DebitInput(
    debit.amount,
    debit.dueDate
  )

  def asTaxpayersSaUtr(saUtr: SaUtr): timetopaytaxpayer.cor.model.SaUtr =
    timetopaytaxpayer.cor.model.SaUtr(saUtr.value)

  implicit def paymentScheduleOps(ps: PaymentSchedule) = new {
    def getMonthlyInstalment: BigDecimal = ps.instalments.head.amount
    def getMonthlyInstalmentDate: Int = ps.instalments.head.paymentDate.getDayOfMonth
    def initialPaymentScheduleDate: LocalDate = ps.instalments.map(_.paymentDate).minBy(_.toEpochDay)
  }
}
