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

import java.io.{File, FileInputStream, FileOutputStream}
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.util.concurrent.atomic.AtomicInteger

import langswitch.{Language, Languages}
import org.openqa.selenium.OutputType.FILE
import org.openqa.selenium.{By, TakesScreenshot, WebDriver}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.selenium.WebBrowser
import play.api.Logger
import testsupport.RichMatchers

final case class BaseUrl(value: String)

object BasePage {
  private val time = now()
  private val seq = new AtomicInteger(0)
  def nextSeq(): Int = seq.getAndIncrement()

  val dumpTargetDir: String = {
    val addon: String = BasePage.time.format(ISO_LOCAL_DATE_TIME)
    s"target/ittests-screenshots-$addon"
  }
}

abstract class BasePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) {
  val richMatchers: RichMatchers = new testsupport.RichMatchers {

    //we shadow what is in testsupport.RichMatchers.patienceConfig
    override val patienceConfig: PatienceConfig = PatienceConfig(
      timeout  = scaled(Span(2, Seconds)),
      interval = scaled(Span(150, Millis))
    )
  }
  import WebBrowser._
  import richMatchers._

  def path: String

  def headingEnglish: String
  def headingWelsh: String

  def expectedTitle(implicit lang: Language = Languages.English): String = lang match {
    case Languages.English => s"$headingEnglish - Set up a Self Assessment payment plan - GOV.UK"
    case Languages.Welsh   => s"$headingWelsh - Trefnu cynllun talu - GOV.UK"
  }

  def findXpath = xpath("//*[@id=\"content\"]/article/section/a")

  def title(): String = pageTitle

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Unit

  def open(): Unit = WebBrowser.goTo(s"${baseUrl.value}$path")

  /**
   * Reads the main content of the page
   */
  def readMain(): String = xpath("""//*[@id="content"]/article""").element.text

  def readGlobalHeaderText(): String = id("proposition-menu").element.text

  def backButtonHref: Option[String] = find(IdQuery("back-link")).fold(Option.empty[String])(e => e.attribute("href"))

  def readPath(): String = new java.net.URL(webDriver.getCurrentUrl).getPath

  def clickOnEnglishLink(): Unit = click on linkText("English")
  def clickOnWelshLink(): Unit = click on linkText("Cymraeg")

  /**
   * Probing tries to run `probingF` until until it succeeds. If it doesn't it:
   * reports what was the page source
   * and dumps page screenshot
   * and fails assertion
   */
  protected def probing[A](probingF: => A): A = eventually(probingF).withClue {
    s"""
       |>>>page text was:
       |${webDriver.findElement(By.tagName("body")).getText}
       |>>>url was: ${webDriver.getCurrentUrl}
       |""".stripMargin
  }

  /**
   * If can it will dump PNG image showing current page in browser.
   *
   * @return some uri of the dumped file or none
   */
  def takeADump(): Option[String] = {

    val fileName: String = s"${this.getClass.getSimpleName}-${BasePage.nextSeq()}.png"
    webDriver match {
      case takesScreenshot: TakesScreenshot =>
        val tmpFile: File = takesScreenshot.getScreenshotAs(FILE)
        new File(BasePage.dumpTargetDir).mkdirs()
        val outFile = new java.io.File(s"${BasePage.dumpTargetDir}/$fileName")
        val fileOutputStream = new FileOutputStream(outFile)
        val inputStream = new FileInputStream(tmpFile)
        fileOutputStream
          .getChannel
          .transferFrom(
            inputStream.getChannel, 0, Long.MaxValue)
        fileOutputStream.close()
        inputStream.close()

        Some(outFile.toURI.toString)
      case _ =>
        Logger(getClass).warn(s"Could not take screen shot: $fileName")
        None
    }
  }

  implicit class StringOps(s: String) {
    /**
     * Transforms string so it's easier it to compare.
     */
    def stripSpaces(): String = s
      .replaceAll("[^\\S\\r\\n]+", " ") //replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") //replace many consecutive new lines with one new line
      .split("\n").map(_.trim) //trim each line
      .filterNot(_ == "") //remove any empty lines
      .mkString("\n")

  }
}
