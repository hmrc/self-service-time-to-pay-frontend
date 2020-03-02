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

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.Calendar

import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.selfservicetimetopay.models._
import testsupport.JsonSyntax._
import timetopaytaxpayer.cor.model._

/**
 * Test Data All
 */
object TdAll {
  val utr = "6573196998"
  val Sautr = SaUtr(utr)

  val debit1 = Debit(
    originCode = "IN1",
    amount     = 2500,
    dueDate    = "2019-11-25",
    interest   = None,
    taxYearEnd = "2020-04-05"
  )

  val debit1Json =
    //language=Json
    """{
    "originCode": "IN1",
     "amount": 2500,
     "dueDate": "2019-11-25",
     "taxYearEnd": "2020-04-05"
    }""".asJson

  val debit2 = Debit(
    originCode = "IN2",
    amount     = 2400,
    dueDate    = "2019-11-25", //TODO: consult with analytics if this data is correct
    interest   = None,
    taxYearEnd = "2020-04-05"
  )

  val debit2Json =
    //language=Json
    """{
    "originCode": "IN2",
     "amount": 2400,
     "dueDate": "2019-11-25",
     "taxYearEnd": "2020-04-05"
    }""".asJson

  val return1 = Return(
    taxYearEnd   = "2020-04-05",
    issuedDate   = "2019-11-10",
    dueDate      = "2019-08-15",
    receivedDate = "2019-03-09"
  )

  val return1Json =
    //language=Json
    """{
    "taxYearEnd": "2020-04-05",
     "issuedDate": "2019-11-10",
     "dueDate": "2019-08-15",
     "receivedDate": "2018-03-09"
    }""".asJson

  val return2 = Return(
    taxYearEnd   = "2018-04-05",
    issuedDate   = "2017-02-15",
    dueDate      = "2018-01-31",
    receivedDate = "2018-03-09"
  )

  val return2Json =
    """{
     "taxYearEnd": "2018-04-05",
     "issuedDate": "2017-02-15",
     "dueDate": "2018-01-31",
     "receivedDate": "2018-03-09"
    }""".asJson

  val communicationPreferences = CommunicationPreferences(
    welshLanguageIndicator = false,
    audioIndicator         = false,
    largePrintIndicator    = false,
    brailleIndicator       = false
  )

  val communicationPreferencesJson =
    //language=Json
    s"""{
       "welshLanguageIndicator": false,
       "audioIndicator": false,
       "largePrintIndicator": false,
       "brailleIndicator": false
       }""".stripMargin

  val selfAssessment = SelfAssessmentDetails(
    utr                      = Sautr,
    communicationPreferences = communicationPreferences,
    debits                   = List(debit1, debit2),
    returns                  = List(return1, return2)
  )

  val selfAssessmentJson =

    s"""{
    "utr": "${utr}",
     "communicationPreferences":${communicationPreferencesJson},
     "debits": [
        ${debit1Json},
        ${debit2Json}]
     ,
     "returns": [
        ${return1Json},
        ${return2Json}]

    }""".asJson

  val address = Address(
    addressLine1 = Some("Big building"),
    addressLine2 = Some("Barington Road"),
    addressLine3 = None,
    addressLine4 = None,
    addressLine5 = None,
    postcode     = Some("BN12 4XL")
  )

  val addressJson =
    //language=Json
    """
    {
     "addressLine1" : "Big building",
     "addressLine2" : "Barington Road",
     "postcode" : "BN12 4XL"
    }
  """.asJson

  val taxpayer = Taxpayer(
    customerName   = "Mr John Campbell",
    addresses      = List(address),
    selfAssessment = selfAssessment
  )

  val taxpayerJson =
    s"""{
     "customerName": "Mr John Campbell",
     "addresses": [${addressJson}],
     "selfAssessment": ${selfAssessmentJson}
    }""".asJson

  val interest = Interest(
    creationDate = Some("2019-10-04"),
    amount       = 200: Double
  )

  val interestJson =
    //language=Json
    """
    {
     "calculationDate": "2019-10-04",
     "amountAccrued": 200
    }
  """.asJson

  val eligibilityStatusJson =
    //language=Json
    """
    {
     "eligible": true,
     "reasons": []
    }
  """.asJson

  def ineligibleStatus(reasons: Seq[Reason]): JsObject =
    s"""
    {
     "eligible": false,
     "reasons": ["${convertReasonsToString(reasons)}"]
    }
  """.asJson

  val noDebits: JsObject = ineligibleStatus(Seq(NoDebt))
  val debtTooSmall: JsObject = ineligibleStatus(Seq(DebtIsInsignificant))
  val oldDebtTooHigh: JsObject = ineligibleStatus(Seq(OldDebtIsTooHigh))
  val totalDebtIsTooHigh: JsObject = ineligibleStatus(Seq(TotalDebtIsTooHigh))
  val arrangementTooShort: JsObject = ineligibleStatus(Seq(TTPIsLessThenTwoMonths))
  val returnNotSubmitted: JsObject = ineligibleStatus(Seq(ReturnNeedsSubmitting))
  val notOnIa: JsObject = ineligibleStatus(Seq(IsNotOnIa))

  def convertReasonsToString(r: Seq[Reason]): String = r.mkString(",")

  val saEnrolment = Enrolment(
    key               = "IR-SA",
    identifiers       = List(
      EnrolmentIdentifier(
        "UTR",
        utr
      )
    ),
    state             = "Activated",
    delegatedAuthRule = None
  )

  val email = "sau@hotmail.com"

  val authToken = "authorization-value"
  val akamaiReputatinoValue = "akamai-reputation-value"
  val requestId = "request-id-value"
  val sessionId = "TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854"
  val trueClientIp = "client-ip"
  val trueClientPort = "client-port"
  val deviceId = "device-id"

  import TdRequest._

  val request = FakeRequest()
    .withSessionId()
    .withLangEnglish()
    .withAuthToken()
    .withAkamaiReputationHeader()
    .withRequestId()
    .withSessionId()
    .withTrueClientIp()
    .withTrueClientPort()
    .withDeviceId()

  implicit def toSome[T](t: T): Option[T] = Some(t)

  implicit def toLocalDate(s: String): LocalDate = LocalDate.parse(s)

  implicit def toOptionLocalDate(s: String): Option[LocalDate] = Some(LocalDate.parse(s))

}
