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

package eligibility.model

import java.time.LocalDate
import play.api.libs.json._

case class EligibilityRequest(dateOfEligibilityCheck: LocalDate, taxpayer: Taxpayer)

case class Taxpayer(customerName: Option[String] = None, addresses: Option[Seq[Address]] = None, selfAssessment: Option[SelfAssessment])

case class Address(
    addressLine1: Option[String],
    addressLine2: Option[String],
    addressLine3: Option[String],
    addressLine4: Option[String],
    addressLine5: Option[String],
    postcode:     Option[String]
)

case class SelfAssessment(utr: Option[String] = None, communicationPreferences: Option[CommunicationPreferences] = None, debits: Option[Seq[Debit]] = None, returns: Option[Seq[Return]] = None)

case class CommunicationPreferences(welshLanguageIndicator: Boolean, audioIndicator: Boolean, largePrintIndicator: Boolean, brailleIndicator: Boolean)

case class Debit(originCode: Option[String] = None, amount: Double, dueDate: LocalDate, interest: Option[Interest] = None, taxYearEnd: Option[LocalDate] = None) {
  def total: Double = interest.map(i => i.amountAccrued).getOrElse(0d) + amount
}

case class Interest(calculationDate: LocalDate, amountAccrued: Double)

case class Return(taxYearEnd: LocalDate, issuedDate: Option[LocalDate] = None, receivedDate: Option[LocalDate] = None, dueDate: Option[LocalDate] = None) {
  def issued(today: LocalDate) = dateHasPassed(issuedDate, today)

  private def dateHasPassed(date: Option[LocalDate], today: LocalDate) = date.isDefined && !date.get.isAfter(today)

  def received(today: LocalDate) = dateHasPassed(receivedDate, today)
}

object EligibilityRequest {
  implicit val reader: Reads[EligibilityRequest] = {
    implicit val readsReturns: Reads[Return] = Json.reads[Return]
    implicit val readsInterest: Reads[Interest] = Json.reads[Interest]
    implicit val readsDebits: Reads[Debit] = Json.reads[Debit]
    implicit val readsCommunicationPreferences: Reads[CommunicationPreferences] = Json.reads[CommunicationPreferences]
    implicit val readsSelfAssessment: Reads[SelfAssessment] = Json.reads[SelfAssessment]
    implicit val readsAddress: Reads[Address] = Json.reads[Address]
    implicit val readsTaxpayer: Reads[Taxpayer] = Json.reads[Taxpayer]

    Json.reads[EligibilityRequest]
  }
}

