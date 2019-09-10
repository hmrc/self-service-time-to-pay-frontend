/*
 * Copyright 2019 HM Revenue & Customs
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

import model.CalculatorPaymentSchedule
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.selfservicetimetopay.models._

final case class Journey(
    _id:                    JourneyId,
    maybeAmount:            Option[BigDecimal]                = None,
    schedule:               Option[CalculatorPaymentSchedule] = None,
    bankDetails:            Option[BankDetails]               = None,
    existingDDBanks:        Option[DirectDebitBank]           = None,
    taxpayer:               Option[Taxpayer]                  = None,
    calculatorData:         CalculatorInput                   = CalculatorInput.initial,
    durationMonths:         Option[Int]                       = Some(2),
    eligibilityStatus:      Option[EligibilityStatus]         = None,
    debitDate:              Option[LocalDate]                 = None,
    notLoggedInJourneyInfo: Option[NotLoggedInJourneyInfo]    = None,
    ddRef:                  Option[String]                    = None) {

  def lengthOfArrangement: Int = schedule.map(_.instalments.length).getOrElse(2)

  def arrangementDirectDebit: Option[ArrangementDirectDebit] = bankDetails.map(f => ArrangementDirectDebit.from(f))

  lazy val amount: BigDecimal = maybeAmount.getOrElse(throw new RuntimeException(s"There was no amount in the journey [${_id}] [${this}]"))
}

object Journey {
  implicit val format: OFormat[Journey] = Json.format[Journey]

  def newJourney(): Journey = Journey(_id = JourneyId.newJourneyId)
}
