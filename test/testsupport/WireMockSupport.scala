package testsupport

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, Suite }
import play.api.Logger

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>

  implicit val wireMockServer: WireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(WireMockSupport.port))

  WireMock.configureFor(WireMockSupport.port)

  override protected def beforeAll(): Unit = wireMockServer.start()

  override protected def afterAll(): Unit = {
    Logger.info("Stopping wire mock server ...")
    wireMockServer.stop()
    Logger.info("Stopping wire mock server - done")
  }

  override def beforeEach() {
    Logger.info("Resetting wire mock server ...")
    WireMock.reset()
    Logger.info("Resetting wire mock server - done")
  }

}

object WireMockSupport {
  val port = 11111
}