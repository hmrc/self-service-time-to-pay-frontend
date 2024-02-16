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

package pagespecs

import testsupport.Language
import testsupport.Language.{English, Welsh}
import play.api.libs.json.{JsObject, Json}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs.{AuditStub, GgStub, TaxpayerStub}
import testsupport.testdata.TdAll.{address, communicationPreferences, saUtr, toLocalDate, toOptionLocalDate}
import timetopaytaxpayer.cor.model.{Debit, Return, SelfAssessmentDetails, Taxpayer}

class CalculatorTaxLiabilitiesPageSpec extends ItSpec {

  override val overrideConfig: Map[String, Any] = Map("auditing.enabled" -> true)

  def beginJourney(): Unit = {
    TaxpayerStub.getTaxpayer()
    GgStub.signInPage(port)
    AuditStub.audit()

    getBanksIsSuccessful()
    startPage.open()
    startPage.clickOnStartNowButton()
  }

  "language" in {
    beginJourney()
    taxLiabilitiesPage.assertInitialPageIsDisplayed

    taxLiabilitiesPage.clickOnWelshLink()
    taxLiabilitiesPage.assertInitialPageIsDisplayed(Welsh)

    taxLiabilitiesPage.clickOnEnglishLink()
    taxLiabilitiesPage.assertInitialPageIsDisplayed(English)

    AuditStub.verifyEventAudited(
      "EligibilityCheck",
      Json.parse(
        """
          |{
          |  "eligibilityResult" : "eligible",
          |  "authProviderId" : "fakeAuthConnectorProviderId",
          |  "totalDebt" : "4900.00",
          |  "utr" : "6573196998",
          |  "taxType" : "SA"
          |}
          |""".stripMargin
      ).as[JsObject]
    )
  }

  "continue to payment-today" in {
    beginJourney()
    taxLiabilitiesPage.assertInitialPageIsDisplayed

    taxLiabilitiesPage.clickOnStartNowButton()
    paymentTodayQuestionPage.assertInitialPageIsDisplayed
  }

  object LateDebitCase {
    private val debit1Amount = 2500
    private val debit2Amount = 2400
    private val taxYearEnd = "2020-04-05"
    private val lateDueDate = "2021-11-25"

    val debit1: Debit = Debit(originCode = "IN1", debit1Amount, dueDate = lateDueDate, interest = None, taxYearEnd)
    val debit2: Debit = Debit(originCode = "IN2", amount = debit2Amount, dueDate = lateDueDate, interest = None, taxYearEnd)

    val taxpayerWithLateDebit: Taxpayer =
      Taxpayer(
        "Mr John Campbell",
        List(address),
        SelfAssessmentDetails(
          saUtr,
          communicationPreferences,
          List(debit1, debit2),
          List(
            Return(taxYearEnd, issuedDate = "2019-11-10", lateDueDate, receivedDate = "2019-03-09"),
            Return(taxYearEnd = "2018-04-05", issuedDate = "2017-02-15", lateDueDate, receivedDate = "2018-03-09"))))

    object Expected {
      object MainText {

        def apply()(implicit language: Language): String = language match {
          case English => mainTextEnglish
          case Welsh   => mainTextWelsh
        }

        private val mainTextEnglish =
          """Your Self Assessment tax bill is £4,900
            |Self Assessment statement
            |Due 25th November 2021
            |First payment on account for tax year 2019 to 2020
            |
            |£2,500
            |(includes interest added to date)
            |
            |Due 25th November 2021
            |Second payment on account for tax year 2019 to 2020
            |
            |£2,400
            |(includes interest added to date)
            |
            |Continue
        """.stripMargin

        private val mainTextWelsh =
          """Mae’ch bil treth Hunanasesiad yn dod i gyfanswm o £4,900
            |Datganiad Hunanasesiad
            |Yn ddyledus erbyn 25th Tachwedd 2021
            |Taliad ar gyfrif cyntaf ar gyfer blwyddyn dreth 2019 i 2020
            |
            |£2,500
            |(yn cynnwys llog a ychwanegwyd hyd yn hyn)
            |
            |Yn ddyledus erbyn 25th Tachwedd 2021
            |Ail daliad ar gyfrif ar gyfer blwyddyn dreth 2019 i 2020
            |
            |£2,400
            |(yn cynnwys llog a ychwanegwyd hyd yn hyn)
            |
            |Yn eich blaen
        """.stripMargin
      }
    }
  }

  "render late debit" in {
    TaxpayerStub.getTaxpayer(LateDebitCase.taxpayerWithLateDebit)
    GgStub.signInPage(port)
    getBanksIsSuccessful()
    AuditStub.audit()

    startPage.open()
    startPage.clickOnStartNowButton()

    import taxLiabilitiesPage._
    val expectedLines = LateDebitCase.Expected.MainText()(English).splitIntoLines()
    taxLiabilitiesPage.assertContentMatchesExpectedLines(expectedLines)

    AuditStub.verifyEventAudited(
      "EligibilityCheck",
      Json.parse(
        """
          |{
          |  "eligibilityResult" : "eligible",
          |  "authProviderId" : "fakeAuthConnectorProviderId",
          |  "totalDebt" : "4900.00",
          |  "utr" : "6573196998",
          |  "taxType" : "SA"
          |}
          |""".stripMargin
      ).as[JsObject]
    )
  }
}
