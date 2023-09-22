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

import crypto.Encrypted
import crypto.model.Encrypted
import journey.Statuses.InProgress
import play.api.libs.json.{Json, OFormat}
import repo.HasId
import ssttpaffordability.model.{Income, Spending}
import ssttparrangement.ArrangementSubmissionStatus
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityStatus, EncryptedBankDetails, EncryptedDirectDebitInstructions, PaymentDayOfMonth, PlanSelection, TypeOfAccountDetails}

import java.time.{LocalDate, LocalDateTime}

final case class EncryptedJourney(
    _id:                              JourneyId,
    status:                           Status                              = InProgress,
    createdOn:                        LocalDateTime,
    maybeTypeOfAccountDetails:        Option[TypeOfAccountDetails]        = None,
    maybeBankDetails:                 Option[EncryptedBankDetails]        = None,
    existingDDBanks:                  Option[EncryptedDirectDebitInstructions]     = None,
    maybeTaxpayer:                    Option[EncryptedTaxpayer]                    = None,
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
) extends HasId[JourneyId] with Encrypted[Journey] {

  override def decrypt: Journey = Journey(
    _id,
    status,
    createdOn,
    maybeTypeOfAccountDetails,
    maybeBankDetails.map(bd => bd.decrypt),
    existingDDBanks.map(ddi => ddi.decrypt),
    maybeTaxpayer.map(tp => tp.decrypt),
    maybePaymentToday,
    maybePaymentTodayAmount,
    maybeIncome,
    maybeSpending,
    maybePlanSelection,
    maybePaymentDayOfMonth,
    maybeEligibilityStatus,
    debitDate,
    ddRef,
    maybeSaUtr,
    maybeArrangementSubmissionStatus
  )

}

object EncryptedJourney {
  implicit val format: OFormat[EncryptedJourney] = Json.format[EncryptedJourney]
}
