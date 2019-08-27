package testsupport


import java.time.{Clock, LocalDateTime, ZoneId, ZoneOffset}

import com.google.inject.{AbstractModule, Provides}
import javax.inject.Singleton
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FreeSpec, TestData}
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}

class ItSpec
  extends FreeSpec
    with GuiceOneServerPerTest
    with RichMatchers
    with WireMockSupport {

  implicit lazy val webDriver: HtmlUnitDriver = {
    val wd = new HtmlUnitDriver(true)
    wd.setJavascriptEnabled(false)
    wd
  }

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(3, Seconds)), interval = scaled(Span(500, Millis)))

  //in tests use `app`
  override def newAppForTest(testData: TestData): Application = new GuiceApplicationBuilder()
    .overrides(GuiceableModule.fromGuiceModules(Seq(overridingsModule)))
    .configure(Map[String, Any](

    )).build()

  def frozenTimeString: String = "2027-11-02T16:33:51.880"

  lazy val overridingsModule: AbstractModule = new AbstractModule {

    override def configure(): Unit = ()

    @Provides
    @Singleton
    def clock: Clock = {
      val fixedInstant = LocalDateTime.parse(frozenTimeString).toInstant(ZoneOffset.UTC)
      Clock.fixed(fixedInstant, ZoneId.systemDefault)
    }
  }
}
