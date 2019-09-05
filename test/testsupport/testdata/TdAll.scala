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

package testsupport.testdata

import java.time.LocalDate

import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.selfservicetimetopay.models._

/**
 * Test Data All
 */
object TdAll {

  val systemDate: LocalDate = "2019-10-04"

  val debit1 = Debit(
    originCode = "IN1",
    amount     = 2500,
    dueDate    = "2019-08-25",
    interest   = None,
    taxYearEnd = "2019-04-05"
  )

  val debit2 = Debit(
    originCode = "IN2",
    amount     = 2400,
    dueDate    = "2019-08-25", //TODO: consult with analytics if this data is correct
    interest   = None,
    taxYearEnd = "2019-04-05"
  )

  val return1 = Return(
    taxYearEnd   = "2019-04-05",
    issuedDate   = "2018-02-15",
    dueDate      = "2019-01-31",
    receivedDate = None
  )

  val return2 = Return(
    taxYearEnd   = "2018-04-05",
    issuedDate   = "2017-02-15",
    dueDate      = "2018-01-31",
    receivedDate = "2018-03-09"
  )

  val communicationPreferences = CommunicationPreferences(
    welshLanguageIndicator = false,
    audioIndicator         = false,
    largePrintIndicator    = false,
    brailleIndicator       = false
  )

  val selfAssessment = SelfAssessment(
    utr                      = utr,
    communicationPreferences = communicationPreferences,
    debits                   = List(debit1, debit2),
    returns                  = List(return1, return2)
  )

  val taxpayer = Taxpayer(
    customerName   = Some("Mr Ethan Johnson"),
    addresses      = List(Address(
      addressLine1 = "Big building",
      addressLine2 = "Barington Road",
      postcode     = "BN12 4XL"
    )),
    selfAssessment = selfAssessment
  )

  val utr = "6573196998"

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
