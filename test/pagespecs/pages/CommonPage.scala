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

package pagespecs.pages

import java.io.{FileInputStream, FileOutputStream}

import org.openqa.selenium.{By, OutputType, TakesScreenshot, WebDriver}
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import play.api.Logger
import testsupport.RichMatchers

import scala.collection.immutable.List
import scala.util.Random

abstract class CommonPage(implicit webDriver: WebDriver) extends RichMatchers with WebBrowser { self =>

  /**
   * A path under which this page is accessible.
   * Don't use url, which contains port number of running play application which is not known at this point
   */
  val path: String

  /**
   * Test routing which verifies that the page has been rendered properly.
   */
  def assertPageIsDisplayed(): Assertion

  final def assertCommonContentIsDisplayed(): Unit = probing {
    readFooterText().text shouldBe expectedFooterTextEnglish
    readGetHelpElement().value.text shouldBe "Get help with this page."
  }

  def readGetHelpElement(): Option[Element] = id("get-help-action").findElement

  def readPath(): String = {
    val url = new java.net.URL(webDriver.getCurrentUrl)
    url.getPath
  }

  /**
   * Expected page title, the value in these tags: <head><title> title is here </head></title>
   */
  def readPageTitle(): String = pageTitle

  def readArticleTitle(): String = xpath("""//*[@id="content"]/article/legend/h1""").element.text

  def readBannerTitle(): String = cssSelector(".header__menu__proposition-name").element.text

  def readFooterText(): Element = id("footer").element

  val expectedFooterTextEnglish: String =
    """Cookies  Privacy policy  Terms and conditions  Help using GOV.UK
      |Open Government Licence
      |All content is available under the Open Government Licence v3.0, except where otherwise stated
      |
      |
      |© Crown Copyright
    """.stripMargin

  def globalErrors: Option[Element] = id("error-summary-display").findElement
  def localError = className("error-message")

  def langSwitchWelsh: Option[Element] = linkText("Cymraeg").findElement
  def langSwitchEnglish: Option[Element] = linkText("English").findElement

  def changeLanguageToWelsh(): Unit = click on langSwitchWelsh.value
  def changeLanguageToEnglish(): Unit = click on langSwitchWelsh.value

  /**
   * The varying part of every page.
   * @return
   */
  def readArticle() = xpath("""//*[@id="content"]/article""").element

  /**
   * Probing tries to run `probingF` until until it succeeds. If it doesn't it:
   * reports what was the page source
   * and dumps page screenshot
   * and fails assertion
   */
  def probing[A](probingF: => A): A = eventually(probingF).withClue {
    val maybeDumpedFile = takeADump()
    s"""
       |${maybeDumpedFile.map(uri => s"Screenshot recorded in $uri").getOrElse("Sorry, no screenshot recorded")}
       |page source was:
       |${webDriver.getPageSource}
       |""".stripMargin
  }

  /**
   * If can it will dump PNG image showing current page in browser.
   *
   * @return some uri of the dumped file or none
   */
  def takeADump(): Option[String] = {
    //original `capture to` relies on side effecting `targetDir`
    //this is safer implementation
    val targetDir = "target/ittests-screenshots"
    val fileName = {
      val addon = List.fill(5)(Random.nextPrintableChar()).mkString
      s"${this.getClass.getSimpleName}-$addon.png"
    }
    webDriver match {
      case takesScreenshot: TakesScreenshot =>
        val tmpFile = takesScreenshot.getScreenshotAs(OutputType.FILE)
        val outFile = new java.io.File(targetDir, fileName)
        new FileOutputStream(outFile)
          .getChannel
          .transferFrom(
            new FileInputStream(tmpFile).getChannel, 0, Long.MaxValue)
        Some(outFile.toURI.toString)
      case _ =>
        Logger(getClass).warn(s"Could not take screen shot: $fileName")
        None
    }
  }

}
