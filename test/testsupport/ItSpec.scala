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
import pagespecs.pages._
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import com.softwaremill.macwire._

class ItSpec
  extends FreeSpec
  with GuiceOneServerPerTest
  with RichMatchers
  with WireMockSupport {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = scaled(Span(300, Millis)), interval = scaled(Span(14, Millis))
  )

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
      "microservice.services.ia.port" -> WireMockSupport.port,
      "microservice.services.auth.port" -> WireMockSupport.port,
      "microservice.services.company-auth.url" -> s"http://localhost:${WireMockSupport.port}",
      "microservice.services.auth.login-callback.base-url" -> s"http://localhost:${port}"

    )).build()

  def frozenTimeString: String = "2019-11-25T16:33:51.880"

  lazy val overridingsModule: AbstractModule = new AbstractModule {

    override def configure(): Unit = ()

    @Provides
    @Singleton
    def clock: Clock = {
      val fixedInstant = LocalDateTime.parse(frozenTimeString).toInstant(ZoneOffset.UTC)
      Clock.fixed(fixedInstant, ZoneId.systemDefault)
    }
  }

  implicit lazy val webDriver: HtmlUnitDriver = {
    val wd = new HtmlUnitDriver(true)
    wd.setJavascriptEnabled(false)
    wd
  }

  lazy val baseUrl: BaseUrl = BaseUrl(s"http://localhost:$port")
  lazy val startPage: StartPage = wire[StartPage]
  lazy val ggSignInPage: GgSignInPage = wire[GgSignInPage]
  lazy val taxLiabilitiesPage: TaxLiabilitiesPage = wire[TaxLiabilitiesPage]
  lazy val debtTooLargePage: DebtTooLargePage = wire[DebtTooLargePage]
  lazy val paymentTodayQuestionPage: PaymentTodayQuestionPage = wire[PaymentTodayQuestionPage]
  lazy val paymentTodayCalculatorPage: PaymentTodayCalculatorPage = wire[PaymentTodayCalculatorPage]
  lazy val monthlyPaymentAmountPage: MonthlyPaymentAmountPage = wire[MonthlyPaymentAmountPage]
  lazy val paymentSummaryPage: PaymentSummaryPage = wire[PaymentSummaryPage]
  lazy val calculatorInstalmentsPage: CalculatorInstalmentsPage = wire[CalculatorInstalmentsPage]
  lazy val instalmentSummarySelectDatePage: InstalmentSummarySelectDatePage = wire[InstalmentSummarySelectDatePage]
  lazy val instalmentSummaryPage: InstalmentSummaryPage = wire[InstalmentSummaryPage]
  lazy val termsAndConditionsPage: TermsAndConditionsPage = wire[TermsAndConditionsPage]
  lazy val directDebitPage: DirectDebitPage = wire[DirectDebitPage]
  lazy val directDebitConfirmationPage: DirectDebitConfirmationPage = wire[DirectDebitConfirmationPage]
  lazy val arrangementSummaryPage: ArrangementSummaryPage = wire[ArrangementSummaryPage]

}
