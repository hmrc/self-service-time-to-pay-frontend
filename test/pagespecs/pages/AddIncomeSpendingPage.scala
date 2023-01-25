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

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertAddLinkIsDisplayed(incomeOrSpending: String)(implicit lang: Language = Languages.English): Unit = {
    incomeOrSpending.toLowerCase() match {
      case "income" => probing {
        assertContentMatchesExpectedLines(Seq(Expected.LinkText.Add.Income()))
      }
      case "spending" => probing {
        assertContentMatchesExpectedLines(Seq(Expected.LinkText.Add.Spending()))
      }
    }
  }

  def assertIncomeTableDisplayed(categoriesFilled: Seq[(String, BigDecimal)] = Seq())(implicit lang: Language): Unit = {
    val expectedCategoryHeadings = Expected.IncomeText.categoryHeadingsText(categoriesFilled.map(_._1))
    val expectedCategoryAmount = Expected.IncomeText.categoryAmounts(categoriesFilled.map(_._2))
    val expectedTotalHeading = Expected.IncomeText.totalIncomeHeadingText
    val expectedTotalAmount = Expected.IncomeText.totalIncomeAmount(categoriesFilled.map(_._2))

    probing {
      assertContentMatchesExpectedLines(
        expectedCategoryHeadings ++
          expectedCategoryAmount :+
          expectedTotalHeading :+
          expectedTotalAmount
      )
    }
  }

  def assertZeroIncomeCategoriesNotDisplayed(
                                          categoriesNotFilled: Seq[String] = Seq()
                                        )(implicit lang: Language): Unit = {
    val expectedCategoryHeadings = Expected.IncomeText.categoryHeadingsText(categoriesNotFilled)
    val expectedCategoryValues = Expected.IncomeText.categoryAmounts(categoriesNotFilled.map(_ => BigDecimal(0)))

    probing {
      assertContentDoesNotContainLines(expectedCategoryHeadings ++ expectedCategoryValues)
    }
  }

  def assertChangeLinkIsDisplayed(incomeOrSpending: String)(implicit lang: Language = Languages.English): Unit = {
    incomeOrSpending.toLowerCase() match {
      case "income" => probing {
        assertContentMatchesExpectedLines(Seq(Expected.LinkText.Change.Income()))
      }
      case "spending" => probing {
        assertContentMatchesExpectedLines(Seq(Expected.LinkText.Change.Spending()))
      }
    }
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
      object Add {

        object Income {
          def apply()(implicit language: Language): String = language match {
            case English => addIncomeTextEnglish
            case Welsh   => addIncomeTextWelsh
          }

          private val addIncomeTextEnglish = "Add income"

          private val addIncomeTextWelsh = "Ychwanegu incwm"
        }

        object Spending {
          def apply()(implicit language: Language): String = language match {
            case English => addSpendingTextEnglish
            case Welsh   => addSpendingTextWelsh
          }

          private val addSpendingTextEnglish = "Add spending"

          private val addSpendingTextWelsh = "Ychwanegu gwariant"

        }
      }
      object Change {
        object Income {
          def apply()(implicit language: Language): String = language match {
            case English => changeIncomeTextEnglish
            case Welsh   => changeIncomeTextWelsh
          }

          private val changeIncomeTextEnglish = "Change income"

          private val changeIncomeTextWelsh = "Newid incwm"
        }
        object Spending {
          def apply()(implicit language: Language): String = language match {
            case English => changeSpendingTextEnglish
            case Welsh   => changeSpendingTextWelsh
          }

          private val changeSpendingTextEnglish = "Change spending"

          private val changeSpendingTextWelsh = "Newid gwariant"
        }
      }
    }

    object IncomeText {
      def categoryHeadingsText(
          categoriesFilled: Seq[String] = Seq()
      )(implicit lang: Language): Seq[String] = {
        categoriesFilled.map {
          case "monthlyIncome" => monthlyIncomeText
          case "benefits"      => benefitsText
          case "otherIncome"   => otherIncomeText
          case (_)             => "nothing"
        }.filterNot(_ == "nothing")
      }

      def categoryAmounts(
          categoryAmounts: Seq[BigDecimal] = Seq()
      )(implicit lang: Language): Seq[String] = {
        categoryAmounts
          .filterNot(_ == 0)
          .map(amount => s"£${commaFormat(amount)}")
      }

      def totalIncomeHeadingText(implicit language: Language) = language match {
        case English => "Total income"
        case Welsh   => "Cyfanswm eich incwm"
      }

      def totalIncomeAmount(categoryAmounts: Seq[BigDecimal] = Seq())(implicit lang: Language): String = {
        commaFormat(categoryAmounts.sum)
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
  }
}
