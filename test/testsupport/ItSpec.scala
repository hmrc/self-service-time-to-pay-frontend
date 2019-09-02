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

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout  = scaled(Span(3, Seconds)), interval = scaled(Span(500, Millis)))

  //in tests use `app`
  override def newAppForTest(testData: TestData): Application = new GuiceApplicationBuilder()
    .overrides(GuiceableModule.fromGuiceModules(Seq(overridingsModule)))
    .configure(Map[String, Any](
      "microservice.services.direct-debit.port" -> WireMockSupport.port,
      "microservice.services.time-to-pay-calculator.port" -> WireMockSupport.port,
      "microservice.services.time-to-pay-arrangement.port" -> WireMockSupport.port,
      "microservice.services.time-to-pay-eligibility.port" -> WireMockSupport.port,
      "microservice.services.time-to-pay-taxpayer.port" -> WireMockSupport.port,
      "microservice.services.campaign-manager.port" -> WireMockSupport.port,
      "microservice.services.ia.port" -> WireMockSupport.port
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
