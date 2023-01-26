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
import ssttpaffordability.model._
import testsupport.RichMatchers._

import java.text.DecimalFormat

class AddIncomeSpendingPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/add-income-spending"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertAddIncomeLinkIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    assertContentMatchesExpectedLines(Seq(Expected.LinkText.AddIncome()))
  }

  def assertAddSpendingLinkIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    assertContentMatchesExpectedLines(Seq(Expected.LinkText.AddSpending()))
  }

  def assertIncomeTableDisplayed(categoriesFilled: IncomeCategory*)(implicit lang: Language): Unit = {
    val expectedCategoryHeadings = Expected.IncomeText.categoryHeadingsText(categoriesFilled)
    val expectedCategoryAmount = Expected.IncomeText.categoryAmounts(categoriesFilled)
    val expectedTotalHeading = Expected.IncomeText.totalIncomeHeadingText
    val expectedTotalAmount = Expected.IncomeText.totalIncomeAmount(categoriesFilled)

    probing {
      assertContentMatchesExpectedLines(
        expectedCategoryHeadings ++
          expectedCategoryAmount :+
          expectedTotalHeading :+
          expectedTotalAmount
      )
    }
  }

  def assertSpendingTableDisplayed(categoriesFilled: Expense*)(implicit lang: Language): Unit = {
    val expectedCategoryHeadings = Expected.SpendingText.categoryHeadingsText(categoriesFilled)
    val expectedCategoryAmount = Expected.SpendingText.categoryAmounts(categoriesFilled)
    val expectedTotalHeading = Expected.SpendingText.totalSpendingHeadingText
    val expectedTotalAmount = Expected.SpendingText.totalSpendingAmount(categoriesFilled)

    probing {
      assertContentMatchesExpectedLines(
        expectedCategoryHeadings ++
          expectedCategoryAmount :+
          expectedTotalHeading :+
          expectedTotalAmount
      )
    }
  }

  def assertZeroIncomeCategoriesNotDisplayed(categoriesNotFilled: IncomeCategory*)(implicit lang: Language): Unit = {
    val categoryHeadingsNotExpected = Expected.IncomeText.categoryHeadingsText(categoriesNotFilled)
    val categoryAmountsNotExpected = Expected.IncomeText.categoryAmounts(categoriesNotFilled)

    probing {
      assertContentDoesNotContainLines(categoryHeadingsNotExpected ++ categoryAmountsNotExpected)
    }
  }

  def assertZeroSpendingCategoriesNotDisplayed(categoriesNotFilled: Expense*)(implicit lang: Language): Unit = {
    val categoryHeadingsNotExpected = Expected.SpendingText.categoryHeadingsText(categoriesNotFilled)
    val categoryAmountsNotExpected = Expected.SpendingText.categoryAmounts(categoriesNotFilled)

    probing {
      assertContentDoesNotContainLines(categoryHeadingsNotExpected ++ categoryAmountsNotExpected)
    }
  }

  def assertChangeIncomeLinkIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    assertContentMatchesExpectedLines(Seq(Expected.LinkText.ChangeIncome()))
  }

  def assertChangeSpendingLinkIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    assertContentMatchesExpectedLines(Seq(Expected.LinkText.ChangeSpending()))
  }

  def assertPathHeaderTitleCorrect(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Add your income and spending"
    case Languages.Welsh   => "Ychwanegu eich incwm a’ch gwariant"
  }

  def clickContinue(): Unit = {
    clickOnContinue()
  }

  def clickOnAddChangeIncome(): Unit = {
    click on id("monthly-income")
  }

  def clickOnAddChangeSpending(): Unit = {
    click on id("monthly-spending")
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
           |1.Income
           |2.Spending
      """.stripMargin

      private val mainTextWelsh =
        s"""Ychwanegu eich incwm a’ch gwariant
           |1.Incwm
           |2.Gwariant
      """.stripMargin
    }

    object LinkText {
      object AddIncome {
        def apply()(implicit language: Language): String = language match {
          case English => addIncomeTextEnglish
          case Welsh   => addIncomeTextWelsh
        }

        private val addIncomeTextEnglish = "Add income"

        private val addIncomeTextWelsh = "Ychwanegu incwm"
      }
      object ChangeIncome {
        def apply()(implicit language: Language): String = language match {
          case English => changeIncomeTextEnglish
          case Welsh   => changeIncomeTextWelsh
        }

        private val changeIncomeTextEnglish = "Change income"

        private val changeIncomeTextWelsh = "Newid incwm"
      }

      object AddSpending {
        def apply()(implicit language: Language): String = language match {
          case English => addSpendingTextEnglish
          case Welsh   => addSpendingTextWelsh
        }

        private val addSpendingTextEnglish = "Add spending"

        private val addSpendingTextWelsh = "Ychwanegu gwariant"
      }

      object ChangeSpending {
        def apply()(implicit language: Language): String = language match {
          case English => changeSpendingTextEnglish
          case Welsh   => changeSpendingTextWelsh
        }

        private val changeSpendingTextEnglish = "Change spending"

        private val changeSpendingTextWelsh = "Newid gwariant"
      }
    }

    object IncomeText {
      def categoryHeadingsText(
          categoriesFilled: Seq[IncomeCategory] = Seq()
      )(implicit lang: Language): Seq[String] = {
        categoriesFilled.map {
          case MonthlyIncome(_) => monthlyIncomeText
          case Benefits(_)      => benefitsText
          case OtherIncome(_)   => otherIncomeText
        }.filterNot(_ == "nothing")
      }

      def categoryAmounts(
          categoryAmounts: Seq[IncomeCategory] = Seq()
      )(implicit lang: Language): Seq[String] = {
        categoryAmounts
          .filterNot(_.amount == 0)
          .map(category => s"£${commaFormat(category.amount)}")
      }

      def totalIncomeHeadingText(implicit language: Language): String = language match {
        case English => "Total income"
        case Welsh   => "Cyfanswm eich incwm"
      }

      def totalIncomeAmount(categoryAmounts: Seq[IncomeCategory] = Seq())(implicit lang: Language): String = {
        commaFormat(categoryAmounts.map(_.amount).sum)
      }

      private def monthlyIncomeText(implicit lang: Language) = lang match {
        case English => "Monthly income after tax"
        case Welsh   => "Incwm misol ar ôl treth"
      }

      private def benefitsText(implicit language: Language) = language match {
        case English => "Benefits"
        case Welsh   => "Budd-daliadau"
      }

      private def otherIncomeText(implicit language: Language) = language match {
        case English => "Other monthly income"
        case Welsh   => "Incwm misol arall"
      }

      private def commaFormat(amount: BigDecimal) = {
        val df = new DecimalFormat("#,##0")
        df.format(amount)
      }

    }

    object SpendingText {
      def categoryHeadingsText(
          categoriesFilled: Seq[Expense] = Seq()
      )(implicit lang: Language): Seq[String] = {
        categoriesFilled.map {
          case Housing(_)              => housingText
          case PensionContributions(_) => pensionContributionsText
          case CouncilTax(_)           => councilTaxText
          case Utilities(_)            => utilitiesText
          case DebtRepayments(_)       => debtRepaymentsText
          case Travel(_)               => travelText
          case Childcare(_)            => childcareText
          case Insurance(_)            => insuranceText
          case Groceries(_)            => groceriesText
          case Health(_)               => healthText
        }.filterNot(_ == "nothing")
      }

      def categoryAmounts(
          categoryAmounts: Seq[Expense] = Seq()
      )(implicit lang: Language): Seq[String] = {
        categoryAmounts
          .filterNot(_.amount == 0)
          .map(category => s"£${commaFormat(category.amount)}")
      }

      def totalSpendingHeadingText(implicit language: Language): String = language match {
        case English => "Total spending"
        case Welsh   => "Cyfanswm eich incwm"
      }

      def totalSpendingAmount(categoryAmounts: Seq[Expense] = Seq())(implicit lang: Language): String = {
        commaFormat(categoryAmounts.map(_.amount).sum)
      }

      private def housingText(implicit lang: Language) = lang match {
        case English => "Housing"
        case Welsh   => "Tai"
      }

      private def pensionContributionsText(implicit language: Language) = language match {
        case English => "Pension contributions"
        case Welsh   => "Cyfraniadau pensiwn"
      }

      private def councilTaxText(implicit language: Language) = language match {
        case English => "Council Tax"
        case Welsh   => "Treth Gyngor"
      }

      private def utilitiesText(implicit language: Language) = language match {
        case English => "utilities"
        case Welsh   => "Cyfleustodau"
      }

      private def debtRepaymentsText(implicit language: Language) = language match {
        case English => "Debt repayments"
        case Welsh   => "Ad-daliadau dyledion"
      }

      private def travelText(implicit language: Language) = language match {
        case English => "Travel"
        case Welsh   => "Teithio"
      }

      private def childcareText(implicit language: Language) = language match {
        case English => "Childcare"
        case Welsh   => "Costau gofal plant"
      }

      private def insuranceText(implicit language: Language) = language match {
        case English => "Insurance"
        case Welsh   => "Yswiriant"
      }

      private def groceriesText(implicit language: Language) = language match {
        case English => "Groceries"
        case Welsh   => "Nwyddau Groser"
      }

      private def healthText(implicit language: Language) = language match {
        case English => "Health"
        case Welsh   => "Iechyd"
      }

      private def commaFormat(amount: BigDecimal) = {
        val df = new DecimalFormat("#,##0")
        df.format(amount)
      }
    }
  }
}
