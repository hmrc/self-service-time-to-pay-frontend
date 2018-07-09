package uk.gov.hmrc.selfservicetimetopay.testsupport

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>
  import WireMockSupport._

  implicit val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(11111))

  WireMock.configureFor(port)

  override protected def beforeAll(): Unit = wireMockServer.start()

  override protected def afterAll(): Unit = wireMockServer.stop()

  override def beforeEach()= WireMock.reset()
}

object WireMockSupport {
  val port = 11111
}
