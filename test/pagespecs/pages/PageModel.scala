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

package pagespecs.pages

import java.io.{FileInputStream, FileOutputStream}

import langswitch.{Language, Languages}
import org.openqa.selenium.{OutputType, TakesScreenshot, WebDriver}
import org.scalatest.Assertion
import org.scalatest.selenium.WebBrowser
import org.scalatest.time.{Millis, Span}
import play.api.Logger

import scala.collection.immutable.List
import scala.util.Random

final case class BaseUrl(value: String)

abstract class Page(baseUrl: BaseUrl)(implicit webDriver: WebDriver) {
  import WebBrowser._
  import testsupport.RichMatchers._

  def path: String

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Assertion

  def open(): Unit = WebBrowser.goTo(s"${baseUrl.value}$path")

  def readPath(): String = {
    val url = new java.net.URL(webDriver.getCurrentUrl)
    url.getPath
  }

  def clickOnEnglishLink(): Unit = click on linkText("English")
  def clickOnWelshLink(): Unit = click on linkText("Cymraeg")

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
       |url was: ${webDriver.getCurrentUrl}
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
