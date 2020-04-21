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

package testsupport.testdata

import java.time.LocalDate

import play.api.libs.json.JsObject
import testsupport.JsonSyntax._
import timetopaytaxpayer.cor.model._
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

/**
 * Test Data All
 */
object TdAll {
  val utr = "6573196998"
  val saUtr = SaUtr(utr)

  private val debit1Amount = 2500
  private val debit2Amount = 2400
  private val taxYearEnd = "2020-04-05"
  private val dueDate = "2019-11-25"

  val debit1 = Debit(originCode = "IN1", debit1Amount, dueDate, interest = None, taxYearEnd)

  val debit1Json: JsObject =
    s"""{
          "originCode": "IN1",
          "amount": $debit1Amount,
          "dueDate": "$dueDate",
          "taxYearEnd": "$taxYearEnd"
        }""".asJson

  //TODO: consult with analytics if this data is correct
  val debit2 = Debit(originCode = "IN2", amount = debit2Amount, dueDate, interest = None, taxYearEnd)

  val debit2Json: JsObject =
    s"""{
          "originCode": "IN2",
          "amount": $debit2Amount,
          "dueDate": "$dueDate",
          "taxYearEnd": "$taxYearEnd"
        }""".asJson

  val communicationPreferences = CommunicationPreferences(
    welshLanguageIndicator = false, audioIndicator = false, largePrintIndicator = false, brailleIndicator = false)

  val address = Address(
    addressLine1 = Some("Big building"),
    addressLine2 = Some("Barington Road"),
    addressLine3 = None,
    addressLine4 = None,
    addressLine5 = None,
    postcode     = Some("BN12 4XL"))

  val taxpayer =
    Taxpayer(
      "Mr John Campbell",
      List(address),
      SelfAssessmentDetails(
        saUtr,
        communicationPreferences,
        List(debit1, debit2),
        List(
          Return(taxYearEnd, issuedDate = "2019-11-10", dueDate = "2019-08-15", receivedDate = "2019-03-09"),
          Return(taxYearEnd   = "2018-04-05", issuedDate = "2017-02-15", dueDate = "2018-01-31", receivedDate = "2018-03-09"))))

  val saEnrolment = Enrolment(
    key               = "IR-SA", identifiers = List(EnrolmentIdentifier("UTR", utr)), state = "Activated", delegatedAuthRule = None)

  val unactivatedSaEnrolment: Enrolment = saEnrolment.copy(state = "Not Activated")

  implicit def toSome[T](t: T): Option[T] = Some(t)

  implicit def toLocalDate(s: String): LocalDate = LocalDate.parse(s)

  implicit def toOptionLocalDate(s: String): Option[LocalDate] = Some(LocalDate.parse(s))
}
