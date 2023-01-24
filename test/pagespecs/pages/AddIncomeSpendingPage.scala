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

package pagespecs.pages

import langswitch.Languages.{English, Welsh}
import langswitch.{Language, Languages}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

import java.text.DecimalFormat

class AddIncomeSpendingPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/add-income-spending"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Add your income and spending"
    case Languages.Welsh   => "Ychwanegu eich incwm a’ch gwariant"
  }

  def clickContinue(): Unit = {
    clickOnContinue()
  }

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def clickOnAddIncome(): Unit = {
    click on id("monthly-income")
  }

  def clickOnAddSpending(): Unit = {
    click on id("monthly-spending")
  }

  def assertIncomeFilled(
      monthlyIncome: BigDecimal = 0,
      benefits:      BigDecimal = 0,
      otherIncome:   BigDecimal = 0
  )(implicit lang: Language): Unit = probing {
    assertPathHeaderTitleCorrect
    val expectedLines = Expected.IncomeFilledText(
      monthlyIncome,
      benefits,
      otherIncome
    )(lang).stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertPathHeaderTitleCorrect(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh   => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish = """Set up a Self Assessment payment plan"""

      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        s"""Add your income and spending
           |Add income
           |1.Income
           |Add spending
           |2.Spending
      """.stripMargin

      private val mainTextWelsh =
        s"""Ychwanegu eich incwm a’ch gwariant
           |Ychwanegu incwm
           |1.Incwm
           |Ychwanegu gwariant
           |2.Gwariant
      """.stripMargin
    }

    object IncomeFilledText {
      def apply(
          monthlyIncome: BigDecimal = 0,
          benefits:      BigDecimal = 0,
          otherIncome:   BigDecimal = 0
      )(implicit lang: Language) = {
        val optionalMonthlyIncomeAmountString = if (monthlyIncome == 0) { "" } else { s"£${commaFormat(monthlyIncome)}" }
        val optionalBenefitsAmountString = if (benefits == 0) { "" } else { s"£${commaFormat(benefits)}" }
        val optionalOtherIncomeAmountString = if (otherIncome == 0) { "" } else { s"£${commaFormat(otherIncome)}" }

        val optionalMonthlyIncomeTextString = if (monthlyIncome == 0) { "" } else { s"$monthlyIncomeText" }
        val optionalBenefitsTextString = if (benefits == 0) { "" } else { s"$benefitsText" }
        val optionalOtherIncomeTextString = if (otherIncome == 0) { "" } else { s"$otherIncomeText"
        }

        val totalIncomeString = s"£${commaFormat(monthlyIncome + benefits + otherIncome)}"

        s"""$optionalMonthlyIncomeTextString
           |$optionalMonthlyIncomeAmountString
           |$optionalBenefitsTextString
           |$optionalBenefitsAmountString
           |$optionalOtherIncomeTextString
           |$optionalOtherIncomeAmountString
           |$totalIncomeText
           |$totalIncomeString
      """.stripMargin
      }

      private def monthlyIncomeText(implicit lang: Language) = {
        println(s"lang at level of monthlyIncomeText $lang")
        lang match {
          case English => "Monthly income after tax"
          case Welsh => "Incwm misol ar ôl treth"
        }
      }

      private def benefitsText(implicit language: Language) = language match {
        case English => "Benefits"
        case Welsh => "Budd-daliadau"
      }

      private def otherIncomeText(implicit language: Language) = language match {
        case English => "Other monthly income"
        case Welsh => "Incwm misol arall"
      }
      private def totalIncomeText(implicit language: Language) = language match {
        case English => "Total income"
        case Welsh => "Cyfanswm eich incwm"
      }



//      private def incomeFilledTextWelsh(
//          monthlyIncome: BigDecimal,
//          benefits:      BigDecimal,
//          otherIncome:   BigDecimal
//      ) = {
//        val totalIncome = monthlyIncome + benefits + otherIncome
//        s"""Ychwanegu eich incwm a’ch gwariant
//           |Ychwanegu incwm
//           |1.Incwm
//           |Incwm misol ar ôl treth
//           |£${commaFormat(monthlyIncome)}
//           |Budd-daliadau
//           |£${commaFormat(benefits)}
//           |Incwm misol arall
//           |£${commaFormat(otherIncome)}
//           |Cyfanswm eich incwm
//           |£${commaFormat(totalIncome)}
//           |Ychwanegu gwariant
//           |2.Gwariant
//      """.stripMargin
//      }

      private def commaFormat(amount: BigDecimal) = {
        val df = new DecimalFormat("#,##0")
        df.format(amount)
      }

    }
  }
}
