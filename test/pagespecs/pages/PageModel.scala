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
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

import langswitch.{Language, Languages}
import org.openqa.selenium.{By, OutputType, TakesScreenshot, WebDriver}
import org.scalatest.Assertion
import org.scalatest.selenium.WebBrowser
import org.scalatest.time.{Millis, Second, Seconds, Span}
import play.api.Logger
import play.api.libs.json.Reads

import scala.collection.immutable.List
import scala.util.Random

final case class BaseUrl(value: String)

object BasePage {
  val time = LocalDateTime.now()
  private val seq = new AtomicInteger(0)
  def nextSeq(): Int = seq.getAndIncrement()

  val dumpTargetDir = {
    val addon: String = BasePage.time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    s"target/ittests-screenshots-$addon"
  }
}

abstract class BasePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) {
  val richMatchers = new testsupport.RichMatchers {

    //we shadow what is in  testsupport.RichMatchers.
    // patienceConfig
    override val patienceConfig: PatienceConfig = PatienceConfig(
      timeout  = scaled(Span(2, Seconds)),
      interval = scaled(Span(150, Millis))
    )
  }
  import richMatchers._
  import WebBrowser._

  def path: String

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Assertion

  def open(): Unit = WebBrowser.goTo(s"${baseUrl.value}$path")

  /**
   * Reads the main content of the page
   */
  def readMain(): String = xpath("""//*[@id="content"]/article""").element.text

  def readGlobalHeader(): String = id("global-header").element.text

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
  protected def probing[A](probingF: => A): A = eventually(probingF).withClue {
    //    val maybeDumpedFile = takeADump()
    //       |>>>page source was:
    //       |${webDriver.getPageSource}
    //       |>>>${maybeDumpedFile.map(uri => s"Screenshot recorded in $uri").getOrElse("Sorry, no screenshot recorded")}

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
        val tmpFile: File = takesScreenshot.getScreenshotAs(OutputType.FILE)
        //     Files.createDirectory(Paths.get("target", "ittests-screenshots"))
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
