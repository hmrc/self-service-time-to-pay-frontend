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
      "microservice.services.direct-debit-backend.port" -> WireMockSupport.port,
      "microservice.services.bars.port" -> WireMockSupport.port,
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
    )
    .build()

  /**
    * This will put into browser the `mdtp` cookie shipped with proper `sessionId` and other required stuff
    * so [[action.AuthenticatedAction]] will be happy and allow for access. Later on this `sessionId` will match
    * the `sessionId` in `Journey`
    */
  protected def login(): Unit = {
    val sessionCookieCrypto: SessionCookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
    AuthResponses.logInResponse(sessionCookieCrypto.crypto)
    webDriver.get(s"http://localhost:${WireMockSupport.port}${AuthResponses.loginPath}")
  }

}
