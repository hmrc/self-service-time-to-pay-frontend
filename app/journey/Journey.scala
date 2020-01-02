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

package journey

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}
import timetopaycalculator.cor.model.{CalculatorInput, PaymentSchedule}
import timetopaytaxpayer.cor.model.Taxpayer
import uk.gov.hmrc.selfservicetimetopay.models._

final case class Journey(
    _id:                    JourneyId,
    maybeAmount:            Option[BigDecimal]        = None,
    schedule:               Option[PaymentSchedule]   = None,
    bankDetails:            Option[BankDetails]       = None,
    existingDDBanks:        Option[DirectDebitBank]   = None,
    maybeTaxpayer:          Option[Taxpayer]          = None,
    maybeCalculatorData:    Option[CalculatorInput]   = None,
    durationMonths:         Int                       = 2,
    maybeEligibilityStatus: Option[EligibilityStatus] = None,
    debitDate:              Option[LocalDate]         = None,
    ddRef:                  Option[String]            = None) {

  def amount: BigDecimal = maybeAmount.getOrElse(throw new RuntimeException(s"Expected 'amount' to be there but was not found. [${_id}] [${this}]"))
  def taxpayer: Taxpayer = maybeTaxpayer.getOrElse(throw new RuntimeException(s"Expected 'Taxpayer' to be there but was not found. [${_id}] [${this}]"))
  def calculatorInput: CalculatorInput = maybeCalculatorData.getOrElse(throw new RuntimeException(s"Expected 'CalculatorData' to be there but was not found. [${_id}] [${this}]"))
  def eligibilityStatus: EligibilityStatus = maybeEligibilityStatus.getOrElse(throw new RuntimeException(s"Expected 'EligibilityStatus' to be there but was not found. [${_id}] [${this}]"))

  def lengthOfArrangement: Int = schedule.map(_.instalments.length).getOrElse(2)
  def arrangementDirectDebit: Option[ArrangementDirectDebit] = bankDetails.map(f => ArrangementDirectDebit.from(f))

}

object Journey {
  implicit val format: OFormat[Journey] = Json.format[Journey]

  def newJourney(): Journey = Journey(_id = JourneyId.newJourneyId)
}
