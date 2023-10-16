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

package testsupport.testdata

import play.api.libs.json.JsObject
import ssttpcalculator.model.AddWorkingDaysResult
import testsupport.JsonSyntax._
import timetopaytaxpayer.cor.model._
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}

/**
 * Test Data All
 */
object TdAll {
  val utr = "6573196998"
  val saUtr: SaUtr = SaUtr(utr)

  val frozenDateString: String = "2019-11-25"
  val aYearAgo: String = "2018-11-25"
  val almostAYearAgo: String = "2018-11-26"

  val frozenDateStringLong: String = "2019-11-25T13:41:01.666"
  val localDateTime: LocalDateTime = LocalDateTime.parse(frozenDateStringLong)
  val instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)

  val dateFirstPaymentCanBeTaken = AddWorkingDaysResult(localDateTime.toLocalDate, 5, localDateTime.toLocalDate.plusDays(10))

  private val debit1Amount = 2500
  private val debit2Amount = 2400
  private val taxYearEnd = "2020-04-05"
  private val dueDate = "2019-11-25"

  val debit1: Debit = Debit(originCode = "IN1", debit1Amount, dueDate, interest = None, taxYearEnd)

  val debit1Json: JsObject =
    s"""{
          "originCode": "IN1",
          "amount": $debit1Amount,
          "dueDate": "$dueDate",
          "taxYearEnd": "$taxYearEnd"
        }""".asJson

  val debit1NddsRejects: Debit = Debit("IN2", 3000, "2023-05-31", interest = None, "2023-04-01")

  val debit1NddsRejectsJson: JsObject =
    s"""{
          "originCode": "IN2",
          "amount": 3000,
          "dueDate": "2023-05-31",
          "taxYearEnd": "2023-04-01"
        }""".asJson

  //TODO: consult with analytics if this data is correct
  val debit2: Debit = Debit(originCode = "IN2", amount = debit2Amount, dueDate, interest = None, taxYearEnd)

  val debit2Json: JsObject =
    s"""{
          "originCode": "IN2",
          "amount": $debit2Amount,
          "dueDate": "$dueDate",
          "taxYearEnd": "$taxYearEnd"
        }""".asJson

  val communicationPreferences: CommunicationPreferences = CommunicationPreferences(
    welshLanguageIndicator = false, audioIndicator = false, largePrintIndicator = false, brailleIndicator = false)

  val address: Address = Address(
    addressLine1 = Some("Big building"),
    addressLine2 = Some("Barington Road"),
    addressLine3 = None,
    addressLine4 = None,
    addressLine5 = None,
    postcode     = Some("BN12 4XL"))

  /*
taxpayer returns and debits so you can plug it into inspector page and recreate journey
todays date: 2019-11-25

{
  "returns" : [ {
    "taxYearEnd" : "2020-04-05",
    "issuedDate" : "2019-11-10",
    "dueDate" : "2019-08-15",
    "receivedDate" : "2019-03-09"
  } ]
}

  {
    "debits" : [ {
    "taxYearEnd" : "2020-04-05",
    "charge" : {
      "originCode" : "IN1",
      "creationDate" : "2020-04-05"
    },
    "relevantDueDate" : "2019-11-25",
    "totalOutstanding" : 2500
  }, {
    "taxYearEnd" : "2020-04-05",
    "charge" : {
      "originCode" : "IN2",
      "creationDate" : "2020-01-01"
    },
    "relevantDueDate" : "2019-11-25",
    "totalOutstanding" : 2400
  } ]
  }
*/
  val taxpayer: Taxpayer =
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

  val taxpayerNddsRejects: Taxpayer =
    Taxpayer(
      "Mr John Campbell",
      List(address),
      SelfAssessmentDetails(
        saUtr,
        communicationPreferences,
        List(debit1NddsRejects),
        List(
          Return("2023-04-05", issuedDate   = "2023-04-20", dueDate = "2023-05-31", receivedDate = "2023-04-10"))))

  val saEnrolment: Enrolment =
    Enrolment(key               = "IR-SA", identifiers = List(EnrolmentIdentifier("UTR", utr)), state = "Activated", delegatedAuthRule = None)

  val unactivatedSaEnrolment: Enrolment = saEnrolment.copy(state = "Not Activated")

  val defaultRemainingIncomeAfterSpending: BigDecimal = BigDecimal(1000)
  val netIncomeTooSmallForPlan = 50
  val netIncomeLargeEnoughForSingleDefaultPlan = 12500
  val netIncomeLargeEnoughForTwoDefaultPlans = 9500

  implicit def toSome[T](t: T): Option[T] = Some(t)

  implicit def toLocalDate(s: String): LocalDate = LocalDate.parse(s)

  implicit def toOptionLocalDate(s: String): Option[LocalDate] = Some(LocalDate.parse(s))
}
