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

import language.Dates
import play.api.i18n.Messages
import timetopaytaxpayer.cor.model.Debit
import timetopaycalculator.cor.model.{DebitInput, Instalment, PaymentSchedule}
import uk.gov.hmrc.domain.SaUtr

package object model {

  def asDebitInput(debit: Debit): DebitInput = DebitInput(
    debit.amount,
    debit.getDueDate()

  )

  def asTaxpayersSaUtr(saUtr: SaUtr): timetopaytaxpayer.cor.model.SaUtr =
    timetopaytaxpayer.cor.model.SaUtr(saUtr.value)

  implicit class PaymentScheduleExt(val ps: PaymentSchedule) extends AnyVal {
    def getMonthlyInstalment: BigDecimal = ps.instalments.head.amount
    def getMonthlyInstalmentDate: Int = ps.instalments.head.paymentDate.getDayOfMonth
    def initialPaymentScheduleDate: LocalDate = ps.instalments.map(_.paymentDate).minBy(_.toEpochDay)
    def getUpFrontPayment: BigDecimal = ps.initialPayment
    def getMonthlyDateFormatted(implicit messages: Messages): String = Dates.getMonthlyDateFormatted(ps.instalments.head.paymentDate)
  }

  implicit class InstalmentExt(val v: Instalment) extends AnyVal {
    def getDateInReadableFormat: String = s"${v.paymentDate.getMonth} ${v.paymentDate.getYear.toString}".toLowerCase.capitalize
  }

  implicit class DebitExt(val v: Debit) extends AnyVal {
    def dueByYear(offset: Int = 0): Int = v.getDueDate().getYear - offset
  }
}
