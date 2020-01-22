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

package uk.gov.hmrc.selfservicetimetopay.models

import java.time.{Clock, LocalDate}

case class Taxpayer(
    customerName:   Option[String]         = None,
    addresses:      Seq[Address]           = Seq.empty,
    selfAssessment: Option[SelfAssessment] = None
) {

  def fixReturns(implicit clock: Clock): Taxpayer = copy(selfAssessment = selfAssessment.map(_.fixReturns))

  def obfuscate: Taxpayer = Taxpayer(
    customerName   = customerName.map(_.replaceAll("[A-Za-z]", "x")),
    addresses      = addresses.map(_.obfuscate),
    selfAssessment = selfAssessment.map(_.obfuscate)
  )
}

case class SelfAssessment(utr:                      Option[String]                   = None,
                          communicationPreferences: Option[CommunicationPreferences] = None,
                          debits:                   Seq[Debit]                       = Seq.empty,
                          returns:                  Option[List[Return]]             = None) {

  /**
   * Removes older than 5 years returns.
   * @return
   */
  def fixReturns(implicit clock: Clock): SelfAssessment = copy(returns = returns.map(_.filter(_.taxYearEnd.isAfter(LocalDate.now(clock).minusYears(5)))))

  def obfuscate: SelfAssessment = SelfAssessment(
    utr                      = utr.map(x => x.take(4) + "***"),
    communicationPreferences = communicationPreferences,
    debits                   = debits,
    returns                  = returns
  )
}

case class Address(addressLine1: Option[String] = None,
                   addressLine2: Option[String] = None,
                   addressLine3: Option[String] = None,
                   addressLine4: Option[String] = None,
                   addressLine5: Option[String] = None,
                   postcode:     Option[String] = None) {

  def obfuscate: Address = Address(
    addressLine1 = addressLine1.map(_.replaceAll("[A-Za-z]", "x")),
    addressLine2 = addressLine2.map(_.replaceAll("[A-Za-z]", "x")),
    addressLine3 = addressLine3.map(_.replaceAll("[A-Za-z]", "x")),
    addressLine4 = addressLine4.map(_.replaceAll("[A-Za-z]", "x")),
    addressLine5 = addressLine5.map(_.replaceAll("[A-Za-z]", "x")),
    postcode     = postcode.map(_.replaceAll("[A-Za-z]", "x"))
  )
}

case class CommunicationPreferences(welshLanguageIndicator: Boolean = false,
                                    audioIndicator:         Boolean = false,
                                    largePrintIndicator:    Boolean = false,
                                    brailleIndicator:       Boolean = false)

// TODO: figure out - is this actually a liability?
final case class Debit(originCode: Option[String]    = None,
                       amount:     BigDecimal,
                       dueDate:    LocalDate,
                       interest:   Option[Interest]  = None,
                       taxYearEnd: Option[LocalDate] = None) {

  def dueByYear: Int = dueDate.getYear
  def dueByMonth: Int = dueDate.getMonthValue
  def dueByDay: Int = dueDate.getDayOfMonth
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
