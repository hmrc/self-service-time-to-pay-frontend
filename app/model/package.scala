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
import ssttpcalculator.model.{DebitInput, Instalment, PaymentSchedule}
import timetopaytaxpayer.cor.model.Debit
import uk.gov.hmrc.domain.SaUtr

package object model {

  def asDebitInput(debit: Debit): DebitInput = DebitInput(
    debit.amount,
    debit.dueDate
  )

  def asTaxpayersSaUtr(saUtr: SaUtr): timetopaytaxpayer.cor.model.SaUtr =
    timetopaytaxpayer.cor.model.SaUtr(saUtr.value)

  implicit class PaymentScheduleExt(val ps: PaymentSchedule) extends AnyVal {
    def firstInstallment: Instalment =
      ps.instalments.reduceOption(first).getOrElse(throw new RuntimeException(s"No installments for [$ps]"))

    def lastInstallment: Instalment =
      ps.instalments.reduceOption(last).getOrElse(throw new RuntimeException(s"No installments for [$ps]"))

    private def first(earliest: Instalment, next: Instalment) =
      if (next.paymentDate.toEpochDay < earliest.paymentDate.toEpochDay) next else earliest

    private def last(latest: Instalment, next: Instalment) =
      if (next.paymentDate.toEpochDay > latest.paymentDate.toEpochDay) next else latest

    def durationInMonths: Int = ps.instalments.size
    def getMonthlyInstalment: BigDecimal = firstInstallment.amount
    def getMonthlyInstalmentDate: Int = firstInstallment.paymentDate.getDayOfMonth
    def initialPaymentScheduleDate: LocalDate = firstInstallment.paymentDate
    def getUpFrontPayment: BigDecimal = ps.initialPayment
    def getMonthlyDateFormatted(implicit messages: Messages): String = Dates.getMonthlyDateFormatted(firstInstallment.paymentDate)
  }

  implicit class InstalmentExt(val v: Instalment) extends AnyVal {
    def getDateInReadableFormat: String = s"${v.paymentDate.getMonth} ${v.paymentDate.getYear.toString}".toLowerCase.capitalize
  }

  implicit class DebitExt(val v: Debit) extends AnyVal {
    def dueByYear(offset: Int = 0): Int = v.dueDate.getYear - offset
  }
}
