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

package pagespecs

import langswitch.Languages
import play.api.libs.json.{Json, Reads}
import testsupport.ItSpec
import testsupport.stubs.{AuthStub, GgStub, TaxpayerStub}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.{Authorization, SessionId}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class StartPageSpec extends ItSpec {

  "debugging auth stub - delete me later" in {
    implicit val hc = HeaderCarrier(
      authorization = Some(Authorization ("Bearer BXQ3/Treo4kQCZvVcCqKPmQujLmLPt2qDNrwaafDRtIVKKjiu+AryxfpbRla6/x8PpMwgG6JxmFNnd7rcszfSua3Zr3ntfNboHxXlAKAvV6YmYYxpJ3muvEfZfXy0dtwL0aVTK+H8QGpi3mqAytEQFIqXQGqNgQEQPE7S+0oLxn9KwIkeIPK/mMlBESjue4V")),
      sessionId     = Some(SessionId("session-2d79a354-e1ce-4a5c-8b71-032111eef698"))
    )
    val json = Json.parse("""{"authorise":[],"retrieve":["allEnrolments","confidenceLevel","saUtr"]}""")

    AuthStub.athorise()

    val http = app.injector.instanceOf[HttpClient]
    val serviceUrl = "http://localhost:8500"

    type A = ~[~[Enrolments, ConfidenceLevel], Option[String]]
    val retrivals: Retrieval[A] = Retrievals.allEnrolments and Retrievals.confidenceLevel and Retrievals.saUtr

    val r = http.POST(s"$serviceUrl/auth/authorise", json).futureValue

    println(Json.prettyPrint(r.json))

    //    implicitly[Reads[~[~[Enrolments, ConfidenceLevel], Option[String]]]]
    r.json.as[~[~[Enrolments, ConfidenceLevel], Option[String]]](retrivals.reads)
  }

  "language" in {

    startPage.open()
    startPage.assertPageIsDisplayed()

    startPage.clickOnWelshLink()
    startPage.assertPageIsDisplayed(Languages.Welsh)

    startPage.clickOnEnglishLink()
    startPage.assertPageIsDisplayed(Languages.English)

  }

  "unuthorised - missing bearer token (user not logged in)" in {
    AuthStub.unathorisedMissingSession()
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    ggSignInPage.assertPageIsDisplayed()
  }

  "eligible" in {
    AuthStub.athorise()
    TaxpayerStub.getTaxpayer()

    //    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    taxLiabilitiesPage.assertPageIsDisplayed()
  }

}
