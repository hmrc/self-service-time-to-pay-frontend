/*
 * Copyright 2022 HM Revenue & Customs
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
import enumeratum.{Enum, EnumEntry}
import enumformat.EnumFormat
import journey.Statuses.{FinishedApplicationSuccessful, InProgress}
import play.api.libs.json.{Format, Json, OFormat}
import repo.HasId
import timetopaytaxpayer.cor.model.{Debit, Taxpayer}
import uk.gov.hmrc.selfservicetimetopay.models.{CalculatorDuration, _}

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

final case class PaymentToday(value: Boolean)

object PaymentToday {
  implicit val format: OFormat[PaymentToday] = Json.format[PaymentToday]
}

final case class PaymentTodayAmount(value: BigDecimal)

object PaymentTodayAmount {
  implicit val format: OFormat[PaymentTodayAmount] = Json.format[PaymentTodayAmount]
}

final case class MonthlyPaymentAmount(value: BigDecimal)

object MonthlyPaymentAmount {
  implicit val format: OFormat[MonthlyPaymentAmount] = Json.format[MonthlyPaymentAmount]
}

final case class Journey(
    _id:              JourneyId,
    status:           Status                          = InProgress,
    createdOn:        LocalDateTime,
    maybeBankDetails: Option[BankDetails]             = None,
    existingDDBanks:  Option[DirectDebitInstructions] = None,

    maybeTaxpayer:              Option[Taxpayer]              = None,
    maybePaymentToday:          Option[PaymentToday]          = None,
    maybePaymentTodayAmount:    Option[PaymentTodayAmount]    = None,
    maybeMonthlyPaymentAmount:  Option[BigDecimal]            = None,
    maybeCalculatorDuration:    Option[CalculatorDuration]    = None,
    maybeArrangementDayOfMonth: Option[ArrangementDayOfMonth] = None,

    maybeEligibilityStatus: Option[EligibilityStatus] = None,
    debitDate:              Option[LocalDate]         = None,
    ddRef:                  Option[String]            = None,
    maybeSaUtr:             Option[String]            = None
) extends HasId[JourneyId] {

  def amount: BigDecimal = maybeMonthlyPaymentAmount.getOrElse(throw new RuntimeException(s"Expected 'amount' to be there but was not found. [${_id}] [$this]"))
  def taxpayer: Taxpayer = maybeTaxpayer.getOrElse(throw new RuntimeException(s"Expected 'Taxpayer' to be there but was not found. [${_id}] [$this]"))
  def debits: Seq[Debit] = taxpayer.selfAssessment.debits

  def requireIsInProgress(): Unit = {
    require(status == InProgress, s"status has to be InProgress [$this]")
  }
  def requireIsEligible(): Unit = {
    require(eligibilityStatus.eligible, s"taxpayer has to be eligible [$this]")
  }

  def requireScheduleIsDefined(): Unit = {
    requireIsInProgress()
    requireIsEligible()

    require(maybeTaxpayer.isDefined, s"'taxpayer' has to be defined at this stage of a journey [$this]")
    require(maybePaymentToday.isDefined, s"'maybePaymentToday' has to be defined at this stage of a journey [$this]")
    require(maybeMonthlyPaymentAmount.isDefined, s"'maybeMonthlyPaymentAmount' has to be defined at this stage of a journey [$this]")
    require(maybeCalculatorDuration.isDefined, s"'maybeCalculatorDuration' has to be defined at this stage of a journey [$this]")
    require(maybeArrangementDayOfMonth.isDefined, s"'maybeArrangementDayOfMonth' has to be defined at this stage of a journey [$this]")
  }

  def requireDdIsDefined(): Unit = {
    requireScheduleIsDefined()
    require(maybeBankDetails.isDefined, s"'maybeBankDetails' has to be defined at this stage of a journey [$this]")
  }

  def paymentToday: Boolean = maybePaymentToday.map(_.value).getOrElse(throw new RuntimeException(s"Expected 'maybePaymentToday' to be there but was not found. [${_id}] [$this]"))
  def initialPayment: BigDecimal = maybePaymentTodayAmount.map(_.value).getOrElse(throw new RuntimeException(s"Expected 'paymentTodayAmount' to be there but was not found. [${_id}] [$this]"))
  def safeInitialPayment: BigDecimal = maybePaymentTodayAmount.map(_.value).getOrElse(0)
  def calculatorDuration: Int = maybeCalculatorDuration.map(_.chosenMonths).getOrElse(throw new RuntimeException(s"Expected 'maybeCalculatorDuration' to be there but was not found. [${_id}] [$this]"))

  def eligibilityStatus: EligibilityStatus =
    maybeEligibilityStatus.getOrElse(throw new RuntimeException(s"Expected 'EligibilityStatus' to be there but was not found. [${_id}] [$this]"))

  def arrangementDirectDebit: Option[ArrangementDirectDebit] = maybeBankDetails.map(f => ArrangementDirectDebit.from(f))

  def bankDetails: BankDetails =
    maybeBankDetails.getOrElse(throw new RuntimeException(s"bank details missing on submission [${_id}]"))

  def saUtr: String = maybeSaUtr.getOrElse(throw new RuntimeException(s"saUtr missing on submission [${_id}]"))

  def inProgress: Boolean = status == InProgress
  def isFinished: Boolean = status == FinishedApplicationSuccessful

  def obfuscate: Journey = Journey(
    _id                       = _id,
    status                    = status,
    createdOn                 = createdOn,
    maybeMonthlyPaymentAmount = maybeMonthlyPaymentAmount,
    maybeBankDetails          = maybeBankDetails.map(_.obfuscate),
    existingDDBanks           = existingDDBanks.map(_.obfuscate),
    maybeTaxpayer             = maybeTaxpayer.map(_.obfuscate),
    maybeEligibilityStatus    = maybeEligibilityStatus,
    debitDate                 = debitDate,
    ddRef                     = ddRef.map(_ => "***"),
    maybeSaUtr                = maybeSaUtr.map(_ => "***")
  )

  override def toString: String = {
    obfuscate.productIterator.mkString(productPrefix + "(", ",", ")")
  }
}

object Journey {
  implicit val format: OFormat[Journey] = Json.format[Journey]

  def newJourney(implicit clock: Clock): Journey = Journey(_id       = JourneyId.newJourneyId(), createdOn = LocalDateTime.now(clock))
}
