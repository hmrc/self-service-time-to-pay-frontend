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

import java.time.{Clock, LocalDate, LocalDateTime}

import controllers.ValueClassBinder.valueClassBinder
import enumeratum.{Enum, EnumEntry}
import enumformat.EnumFormat
import langswitch.Languages.findValues
import play.api.i18n.{Lang, Messages}
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.PathBindable
import ssttpcalculator.CalculatorPaymentScheduleExt
import timetopaycalculator.cor.model.{CalculatorInput, PaymentSchedule}
import timetopaytaxpayer.cor.model.Taxpayer
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.collection.immutable

sealed trait Status extends EnumEntry

object Status {
  implicit val format: Format[Status] = EnumFormat(Statuses)
}

object Statuses extends Enum[Status] {

  case object InProgress extends Status
  case object FinishedApplicationSuccessful extends Status

  override def values: immutable.IndexedSeq[Status] = findValues
}

final case class Journey(
    _id:                    JourneyId,
    status:                 Status                               = Statuses.InProgress,
    createdOn:              LocalDateTime,
    maybeAmount:            Option[BigDecimal]                   = None,
    schedule:               Option[CalculatorPaymentScheduleExt] = None,
    bankDetails:            Option[BankDetails]                  = None,
    existingDDBanks:        Option[DirectDebitBank]              = None,
    maybeTaxpayer:          Option[Taxpayer]                     = None,
    maybeCalculatorData:    Option[CalculatorInput]              = None,
    durationMonths:         Int                                  = 2,
    maybeEligibilityStatus: Option[EligibilityStatus]            = None,
    debitDate:              Option[LocalDate]                    = None,
    ddRef:                  Option[String]                       = None
) {

  def amount: BigDecimal = maybeAmount.getOrElse(throw new RuntimeException(s"Expected 'amount' to be there but was not found. [${_id}] [${this}]"))
  def taxpayer: Taxpayer = maybeTaxpayer.getOrElse(throw new RuntimeException(s"Expected 'Taxpayer' to be there but was not found. [${_id}] [${this}]"))
  def calculatorInput: CalculatorInput = maybeCalculatorData.getOrElse(throw new RuntimeException(s"Expected 'CalculatorData' to be there but was not found. [${_id}] [${this}]"))
  def eligibilityStatus: EligibilityStatus = maybeEligibilityStatus.getOrElse(throw new RuntimeException(s"Expected 'EligibilityStatus' to be there but was not found. [${_id}] [${this}]"))

  def lengthOfArrangement: Int = schedule.map(_.schedule.instalments.length).getOrElse(2)
  def arrangementDirectDebit: Option[ArrangementDirectDebit] = bankDetails.map(f => ArrangementDirectDebit.from(f))

  def obfuscate: Journey = Journey(
    _id                    = _id,
    createdOn              = createdOn,
    maybeAmount            = maybeAmount,
    schedule               = schedule,
    bankDetails            = bankDetails.map(_.obfuscate),
    existingDDBanks        = existingDDBanks.map(_.obfuscate),
    maybeTaxpayer          = maybeTaxpayer.map(_.obfuscate),
    maybeCalculatorData    = maybeCalculatorData,
    durationMonths         = durationMonths,
    maybeEligibilityStatus = maybeEligibilityStatus,
    debitDate              = debitDate,
    ddRef                  = ddRef.map(_ => "***")
  )
}

object Journey {
  implicit val format: OFormat[Journey] = Json.format[Journey]

  def newJourney(implicit clock: Clock): Journey = Journey(_id       = JourneyId.newJourneyId(), createdOn = LocalDateTime.now(clock))
}
