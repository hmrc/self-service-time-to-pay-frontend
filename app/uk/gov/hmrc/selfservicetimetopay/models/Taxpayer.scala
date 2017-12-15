/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.LocalDate

case class Taxpayer(
  customerName: Option[String] = None,
  addresses: Seq[Address] = Seq.empty,
  selfAssessment: Option[SelfAssessment] = None
)

case class SelfAssessment(utr: Option[String] = None,
                          communicationPreferences: Option[CommunicationPreferences] = None,
                          debits: Seq[Debit] = Seq.empty,
                          returns: Option[List[Return]] = None)

case class Address(addressLine1: Option[String] = None,
                   addressLine2: Option[String] = None,
                   addressLine3: Option[String] = None,
                   addressLine4: Option[String] = None,
                   addressLine5: Option[String] = None,
                   postcode: Option[String] = None)

case class CommunicationPreferences(welshLanguageIndicator: Boolean = false,
                                     audioIndicator: Boolean = false,
                                     largePrintIndicator: Boolean = false,
                                     brailleIndicator: Boolean = false)

case class Debit(originCode: Option[String] = None,
                 amount: BigDecimal,
                 dueDate: LocalDate,
                 interest: Option[Interest] = None,
                 taxYearEnd: Option[LocalDate] = None) {

  def dueByYear = dueDate.getYear
  def dueByMonth = dueDate.getMonthValue
  def dueByDay = dueDate.getDayOfMonth
}

object CalculatorAmountDue {
  val MaxCurrencyValue: BigDecimal = BigDecimal.exact("1e5")

  def apply(amt: BigDecimal, dueByYear: Int, dueByMonth: Int, dueByDay: Int): Debit = {
    Debit(None, amt, LocalDate.of(dueByYear, dueByMonth, dueByDay), None, None)
  }

  def unapply(arg: Debit): Option[(BigDecimal, Int, Int, Int)] = {
    Some((arg.amount, arg.dueDate.getYear, arg.dueDate.getMonthValue, arg.dueDate.getDayOfMonth))
  }
}

case class Return(taxYearEnd: LocalDate, issuedDate: Option[LocalDate], dueDate: Option[LocalDate], receivedDate: Option[LocalDate])

case class Interest(calculationDate: LocalDate, amountAccrued: BigDecimal)
