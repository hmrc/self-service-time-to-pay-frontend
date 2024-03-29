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

import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import ssttpaffordability.model.Expense._
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import ssttpaffordability.model._
import testsupport.Language
import testsupport.Language.{English, Welsh}
import testsupport.RichMatchers._

import java.text.DecimalFormat

class AddIncomeSpendingPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/add-income-spending"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertPagePathCorrect: Assertion = probing {
    readPath() shouldBe path
  }

  def assertAddIncomeLinkIsDisplayed(implicit lang: Language = Language.English): Unit = probing {
    assertContentMatchesExpectedLines(Seq(Expected.LinkText.AddIncome()))
  }

  def assertAddSpendingLinkIsDisplayed(implicit lang: Language = Language.English): Unit = probing {
    assertContentMatchesExpectedLines(Seq(Expected.LinkText.AddSpending()))
  }

  def assertIncomeTableDisplayed(budgetLines: IncomeBudgetLine*)(implicit lang: Language): Unit = {
    val expectedCategoryHeadings = Expected.IncomeText.categoryHeadingsText(budgetLines.map(_.category))
    val expectedCategoryAmount = Expected.IncomeText.categoryAmounts(budgetLines)
    val expectedTotalHeading = Expected.IncomeText.totalIncomeHeadingText
    val expectedTotalAmount = Expected.IncomeText.totalIncomeAmount(budgetLines)

    probing {
      assertContentMatchesExpectedLines(
        expectedCategoryHeadings ++
          expectedCategoryAmount :+
          expectedTotalHeading :+
          expectedTotalAmount
      )
    }
  }

  def assertSpendingTableDisplayed(categoriesFilled: Expenses*)(implicit lang: Language): Unit = {
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

  def assertZeroIncomeCategoriesDisplayed(categoriesNotFilled: IncomeCategory*)(implicit lang: Language): Unit = {
    val categoryHeadingsExpected = Expected.IncomeText.categoryHeadingsText(categoriesNotFilled)
    val categoryAmountsExpected = Expected.IncomeText.categoryAmounts(categoriesNotFilled.map(IncomeBudgetLine(_)))

    probing {
      assertContentMatchesExpectedLines(categoryHeadingsExpected ++ categoryAmountsExpected)
    }
  }

  def assertZeroSpendingCategoriesDisplayed(categoriesNotFilled: Expenses*)(implicit lang: Language): Unit = {
    val categoryHeadingsExpected = Expected.SpendingText.categoryHeadingsText(categoriesNotFilled)
    val categoryAmountsExpected = Expected.SpendingText.categoryAmounts(categoriesNotFilled)

    probing {
      assertContentMatchesExpectedLines(categoryHeadingsExpected ++ categoryAmountsExpected)
    }
  }

  def assertChangeIncomeLinkIsDisplayed(implicit lang: Language = Language.English): Unit = probing {
    assertContentMatchesExpectedLines(Seq(Expected.LinkText.ChangeIncome()))
  }

  def assertChangeSpendingLinkIsDisplayed(implicit lang: Language = Language.English): Unit = probing {
    assertContentMatchesExpectedLines(Seq(Expected.LinkText.ChangeSpending()))
  }

  def assertPathHeaderTitleCorrect(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Language.English => "Add your income and spending"
    case Language.Welsh   => "Ychwanegu eich incwm a’ch gwariant"
  }

  def enterIncome(amount: String): Unit = {
    clickOnAddChangeIncome()
    enterIncomeAmount(amount)
    clickContinue()
  }

  def enterIncomeAmount(value: String): Unit = {
    val amount = xpath("//*[@id=\"monthlyIncome\"]")
    click on amount
    enter(value)
  }

  def enterSpending(amount: String): Unit = {
    clickOnAddChangeSpending()
    enterSpendingAmount(amount)
    clickContinue()
  }

  def enterSpendingAmount(value: String): Unit = {
    val amount = xpath("//*[@id=\"housing\"]")
    click on amount
    enter(value)
  }

  def clickContinue(): Unit =
    clickOnContinue()

  def clickOnAddChangeIncome(): Unit =
    click on id("monthly-income")

  def clickOnAddChangeSpending(): Unit =
    click on id("monthly-spending")

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
          case MonthlyIncome => monthlyIncomeText
          case Benefits      => benefitsText
          case OtherIncome   => otherIncomeText
        }.filterNot(_ == "nothing")
      }

      def categoryAmounts(
          categoryAmounts: Seq[IncomeBudgetLine] = Seq()
      ): Seq[String] = {
        categoryAmounts
          .filterNot(_.amount == 0)
          .map(category => s"£${commaFormat(category.amount)}")
      }

      def totalIncomeHeadingText(implicit language: Language): String = language match {
        case English => "Total income"
        case Welsh   => "Cyfanswm eich incwm"
      }

      def totalIncomeAmount(categoryAmounts: Seq[IncomeBudgetLine] = Seq()): String = {
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
          categoriesFilled: Seq[Expenses] = Seq()
      )(implicit lang: Language): Seq[String] = {
        categoriesFilled.map {
          _.category match {
            case HousingExp              => housingText
            case PensionContributionsExp => pensionContributionsText
            case CouncilTaxExp           => councilTaxText
            case UtilitiesExp            => utilitiesText
            case DebtRepaymentsExp       => debtRepaymentsText
            case TravelExp               => travelText
            case ChildcareExp            => childcareText
            case InsuranceExp            => insuranceText
            case GroceriesExp            => groceriesText
            case HealthExp               => healthText
          }
        }.filterNot(_ == "nothing")
      }

      def categoryAmounts(categoryAmounts: Seq[Expenses] = Seq()): Seq[String] = {
        categoryAmounts
          .filterNot(_.amount == 0)
          .map(category => s"£${commaFormat(category.amount)}")
      }

      def totalSpendingHeadingText(implicit language: Language): String = language match {
        case English => "Total spending"
        case Welsh   => "Cyfanswm y gwariant"
      }

      def totalSpendingAmount(categoryAmounts: Seq[Expenses] = Seq()): String = {
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
        case English => "Council tax"
        case Welsh   => "Treth Gyngor"
      }

      private def utilitiesText(implicit language: Language) = language match {
        case English => "Utilities"
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
