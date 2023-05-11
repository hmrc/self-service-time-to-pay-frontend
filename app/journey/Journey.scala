/*
 * Copyright 2023 HM Revenue & Customs
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

import enumeratum.{Enum, EnumEntry}
import enumformat.EnumFormat
import journey.Statuses.{ApplicationComplete, InProgress}
import play.api.libs.json.{Format, Json, OFormat, Reads, Writes, __}
import repo.HasId
import ssttpaffordability.model.Income
import ssttpaffordability.model.Spending
import ssttparrangement.ArrangementSubmissionStatus
import timetopaytaxpayer.cor.model.{Debit, Taxpayer}
import uk.gov.hmrc.selfservicetimetopay.models._

import java.time.{Clock, Instant, LocalDate, LocalDateTime, ZoneOffset}
import scala.collection.immutable

sealed trait Status extends EnumEntry

object Status {
  implicit val format: Format[Status] = EnumFormat(Statuses)
}

object Statuses extends Enum[Status] {

  case object InProgress extends Status
  case object ApplicationComplete extends Status

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

final case class Journey(
    _id:                              JourneyId,
    status:                           Status                              = InProgress,
    createdOn:                        LocalDateTime,
    maybeTypeOfAccountDetails:        Option[TypeOfAccountDetails]        = None,
    maybeBankDetails:                 Option[BankDetails]                 = None,
    existingDDBanks:                  Option[DirectDebitInstructions]     = None,
    maybeTaxpayer:                    Option[Taxpayer]                    = None,
    maybePaymentToday:                Option[PaymentToday]                = None,
    maybePaymentTodayAmount:          Option[PaymentTodayAmount]          = None,
    maybeIncome:                      Option[Income]                      = None,
    maybeSpending:                    Option[Spending]                    = None,
    maybePlanSelection:               Option[PlanSelection]               = None,
    maybePaymentDayOfMonth:           Option[PaymentDayOfMonth]           = None,
    maybeEligibilityStatus:           Option[EligibilityStatus]           = None,
    debitDate:                        Option[LocalDate]                   = None,
    ddRef:                            Option[String]                      = None,
    maybeSaUtr:                       Option[String]                      = None,
    maybeArrangementSubmissionStatus: Option[ArrangementSubmissionStatus] = None
) extends HasId[JourneyId] {

  def maybeSelectedPlanAmount: Option[BigDecimal] = maybePlanSelection.fold(None: Option[BigDecimal])(_.selection match {
    case Right(CustomPlanRequest(_)) => None
    case Left(SelectedPlan(amount))  => Some(amount)
  })

  def selectedPlanAmount: BigDecimal = maybeSelectedPlanAmount.getOrElse(
    throw new RuntimeException(s"Expected selected plan amount but none found. [${_id}] [$this]")
  )
  def taxpayer: Taxpayer = maybeTaxpayer.getOrElse(throw new RuntimeException(s"Expected 'Taxpayer' to be there but was not found. [${_id}] [$this]"))
  def debits: Seq[Debit] = taxpayer.selfAssessment.debits
  def requireIsInProgress(): Unit = {
    require(status == InProgress, s"status has to be InProgress [$this]")
  }
  def requireIsEligible(): Unit = {
    require(eligibilityStatus.eligible, s"taxpayer has to be eligible [$this]")
  }

  def selectedDay: Option[Int] = maybePaymentDayOfMonth.map(_.dayOfMonth)

  def remainingIncomeAfterSpending: BigDecimal = {
    val totalIncome = maybeIncome.map(_.totalIncome).getOrElse(throw new IllegalArgumentException("attempted to retrieve total income when there was no income"))
    val totalSpending = maybeSpending.map(_.totalSpending).getOrElse(throw new IllegalArgumentException("attempted to retrieve total spending when there was no spending"))
    totalIncome - totalSpending
  }

  def requireScheduleIsDefined(): Unit = {
    requireIsInProgress()
    requireIsEligible()

    require(maybeTaxpayer.isDefined, s"'taxpayer' has to be defined at this stage of a journey [$this]")
    require(maybePaymentToday.isDefined, s"'maybePaymentToday' has to be defined at this stage of a journey [$this]")
    require(maybeIncome.isDefined, s"'maybeIncome' has to be defined at this stage of a journey [$this]")
    require(maybeSpending.isDefined, s"'maybeSpending has to be defined at this stage of a journey [$this]")
    require(maybeSelectedPlanAmount.isDefined, s"'maybeSelectedPlanAmount' has to be defined at this stage of a journey [$this]")
    require(maybePaymentDayOfMonth.isDefined, s"'maybePaymentDayOfMonth' has to be defined at this stage of a journey [$this]")
  }

  def requireDdIsDefined(): Unit = {
    requireScheduleIsDefined()
    require(maybeBankDetails.isDefined, s"'maybeBankDetails' has to be defined at this stage of a journey [$this]")
  }

  def requireIsAccountHolder(): Unit = {
    require(maybeTypeOfAccountDetails.map(_.isAccountHolder).contains(true), s"isAccountHolder must be true to continue at this stage of the journey")
  }

  def paymentToday: Boolean = maybePaymentToday.map(_.value).getOrElse(throw new RuntimeException(s"Expected 'maybePaymentToday' to be there but was not found. [${_id}] [$this]"))
  def upfrontPayment: BigDecimal = maybePaymentTodayAmount.map(_.value).getOrElse(throw new RuntimeException(s"Expected 'paymentTodayAmount' to be there but was not found. [${_id}] [$this]"))
  def safeUpfrontPayment: BigDecimal = maybePaymentTodayAmount.map(_.value).getOrElse(0)
  def eligibilityStatus: EligibilityStatus =
    maybeEligibilityStatus.getOrElse(throw new RuntimeException(s"Expected 'EligibilityStatus' to be there but was not found. [${_id}] [$this]"))

  def arrangementDirectDebit: Option[ArrangementDirectDebit] = maybeBankDetails.map(f => ArrangementDirectDebit.from(f))

  def bankDetails: BankDetails =
    maybeBankDetails.getOrElse(throw new RuntimeException(s"bank details missing on submission [${_id}]"))

  def saUtr: String = maybeSaUtr.getOrElse(throw new RuntimeException(s"saUtr missing on submission [${_id}]"))

  def inProgress: Boolean = status == InProgress
  def isFinished: Boolean = status == ApplicationComplete

  def obfuscate: Journey = Journey(
    _id                              = _id,
    status                           = status,
    createdOn                        = createdOn,
    maybeTypeOfAccountDetails        = maybeTypeOfAccountDetails,
    maybeBankDetails                 = maybeBankDetails.map(_.obfuscate),
    existingDDBanks                  = existingDDBanks.map(_.obfuscate),
    maybeTaxpayer                    = maybeTaxpayer.map(_.obfuscate),
    maybePaymentToday                = maybePaymentToday,
    maybePaymentTodayAmount          = maybePaymentTodayAmount,
    maybeIncome                      = maybeIncome,
    maybeSpending                    = maybeSpending,
    maybePlanSelection               = maybePlanSelection,
    maybePaymentDayOfMonth           = maybePaymentDayOfMonth,
    maybeEligibilityStatus           = maybeEligibilityStatus,
    debitDate                        = debitDate,
    ddRef                            = ddRef.map(_ => "***"),
    maybeSaUtr                       = maybeSaUtr.map(_ => "***"),
    maybeArrangementSubmissionStatus = maybeArrangementSubmissionStatus
  )

  override def toString: String = {
    obfuscate.productIterator.mkString(productPrefix + "(", ",", ")")
  }
}

object Journey {
  final val localDateTimeReads: Reads[LocalDateTime] =
    Reads.at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

  final val localDateTimeWrites: Writes[LocalDateTime] =
    Writes.at[String](__ \ "$date" \ "$numberLong")
      .contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)

  implicit val localDateTimeFormat: Format[LocalDateTime] = Format(localDateTimeReads, localDateTimeWrites)

  implicit val format: OFormat[Journey] = Json.format[Journey]

  def newJourney(implicit clock: Clock): Journey = Journey(_id       = JourneyId.newJourneyId(), createdOn = LocalDateTime.now(clock))
}
