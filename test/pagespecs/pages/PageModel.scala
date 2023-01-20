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

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Unit

  def assertContentMatchesExpectedLines(expectedLines: Seq[String]): Unit = probing {
    //we replace `\n` with spaces so the tests can run both in intellij and in sbt.
    //for some reasons webDeriver's `getText` returns text with extra new lines if you run it from intellij.
    val content = readMain().stripSpaces().replaceAll("\n", " ")
    expectedLines.foreach { expectedLine =>
      withClue(s"The page content should include '$expectedLine'"){
        content should include(expectedLine)
      }
    }
  }
  def open(): Unit = WebBrowser.goTo(s"${baseUrl.value}$path")

  def expectedTitle(heading: String, lang: Language): String = lang match {
    case Languages.English => s"$heading - Set up a Self Assessment payment plan - GOV.UK"
    case Languages.Welsh   => s"$heading - Trefnu cynllun talu - GOV.UK"
  }

  /**
   * Reads the main content of the page
   */
  def readMain(): String = xpath("""//*[@id="content"]""").element.text

  def readGlobalHeaderText(): String = className("hmrc-header__service-name").element.text

  def href(id: String): Option[String] = find(IdQuery(id)).fold(Option.empty[String])(e => e.attribute("href"))
  def backButtonHref: Option[String] = href("back-link")

  def readPath(): String = new java.net.URL(webDriver.getCurrentUrl).getPath

  def clickOnEnglishLink(): Unit = click on className("govuk-link")

  def clickOnWelshLink(): Unit = click on className("govuk-link")

  def clickOnContinue(): Unit = click on id("continue")

  def clickOnTempButton(): Unit = click on id("temp")

  /**
   * Probing tries to run `probingF` until until it succeeds. If it doesn't it:
   * reports what was the page source
   * and dumps page screenshot
   * and fails assertion
   */
  protected def probing[A](probingF: => A): A = eventually(probingF).withClue {
    s"""
       |>>>url was: ${webDriver.getCurrentUrl}
       |>>>path is supposed to be: $path
       |>>>page text was:
       |${webDriver.findElement(By.tagName("body")).getText}

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
     * It also replaces `unchecked`
     *
     */
    def stripSpaces(): String = s
      .replaceAll("unchecked", "") //when you run tests from intellij webdriver.getText adds extra 'unchecked' around selection
      .replaceAll("[^\\S\\r\\n]+", " ") //replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") //replace many consecutive new lines with one new line
      .split("\n").map(_.trim) //trim each line
      .filterNot(_ == "") //remove any empty lines
      .mkString("\n")
  }
}
