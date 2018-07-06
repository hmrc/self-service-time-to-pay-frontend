package uk.gov.hmrc.selfservicetimetopay.testsupport

import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.scalatest.FreeSpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class BrowserSpec extends FreeSpec
  with GuiceOneServerPerSuite
  with WireMockSupport
  with RichMatchersIt {

  //TODO: change this to something faster. phantomjs could be an option
  protected implicit val webDriver: WebDriver = new HtmlUnitDriver(true)

  protected val baseUrl = s"http://localhost:$port"

  def goTo(path: String) = webDriver.get(s"$baseUrl$path")

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> WireMockSupport.port,
      "microservice.services.direct-debit.port" -> WireMockSupport.port,
      "microservice.services.time-to-pay-calculator.port" -> WireMockSupport.port,
      "microservice.services.time-to-pay-arrangement.port" -> WireMockSupport.port,
      "microservice.services.time-to-pay-eligibility.port" -> WireMockSupport.port,
      "microservice.services.time-to-pay-taxpayer.port" -> WireMockSupport.port
    )
    .build()

}
