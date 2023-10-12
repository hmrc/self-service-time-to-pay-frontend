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

package testsupport

import com.google.inject.{AbstractModule, Provides}
import com.softwaremill.macwire._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import pagespecs.pages._
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.test.{DefaultTestServerFactory, RunningServer}
import play.api.{Application, Mode}
import play.core.server.ServerConfig
import testsupport.testdata.TdAll.frozenDateString
import times.ClockProvider
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances}

import java.time.ZoneOffset.UTC
import java.time.{Clock, LocalDateTime, ZoneId}
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import play.api.inject.bind
import uk.gov.hmrc.mongo.MongoComponent

class ItSpec
  extends AnyFreeSpec
  with GuiceOneServerPerSuite
  with RichMatchers
  with WireMockSupport
  with HttpReadsInstances {

  val testPort: Int = 19001

  val baseUrl: BaseUrl = BaseUrl(s"http://localhost:$testPort")

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout  = scaled(Span(300, Millis)), interval = scaled(Span(2, Seconds)))

  protected val overrideConfig: Map[String, Any] = Map.empty

  protected lazy val configMap: Map[String, Any] = Map[String, Any](
    "mongodb.uri" -> "mongodb://localhost:27017/self-service-time-to-pay-frontend-tests",
    "microservice.services.direct-debit.port" -> WireMockSupport.port,
    "microservice.services.date-calculator.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-arrangement.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-taxpayer.port" -> WireMockSupport.port,
    "microservice.services.ia.port" -> WireMockSupport.port,
    "microservice.services.auth.port" -> WireMockSupport.port,
    "microservice.services.company-auth.url" -> s"http://localhost:${WireMockSupport.port}",
    "microservice.services.auth.login-callback.base-url" -> s"http://localhost:${testPort}",
    "microservice.services.add-taxes.port" -> WireMockSupport.port,
    "microservice.services.bars.port" -> WireMockSupport.port,
    "microservice.services.identity-verification-frontend.uplift-url" -> s"http://localhost:${WireMockSupport.port}/mdtp/uplift",
    "microservice.services.identity-verification-frontend.callback.base-url" -> s"http://localhost:${testPort}",
    "microservice.services.identity-verification-frontend.callback.complete-path" -> "/pay-what-you-owe-in-instalments/arrangement/determine-eligibility",
    "microservice.services.identity-verification-frontend.callback.reject-path" -> "/pay-what-you-owe-in-instalments/eligibility/not-enrolled",
    "auditing.consumer.baseUri.port" -> WireMockSupport.port,
    "auditing.enabled" -> false,
    "logger.root" -> "WARN",
    "play.http.router" -> "testOnlyDoNotUseInAppConf.Routes"
  ) ++ overrideConfig

  val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

      val retrievalResult = Future.successful(
        new ~(new ~(Enrolments(Set(Enrolment("IR-SA"))), Some("6573196998")), Some(Credentials("IR-SA", "")))
      )
      (retrievalResult.map(_.asInstanceOf[A]))

    }
  }
  //in tests use `app`
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].toInstance(fakeAuthConnector))
    .overrides(GuiceableModule.fromGuiceModules(Seq(module)))
    .configure(configMap)

    .build()

  val frozenTimeString: String = s"${frozenDateString}T16:33:51.880"

  lazy val module: AbstractModule = new AbstractModule {
    override def configure(): Unit = ()

    @Provides
    @Singleton
    def clockProvider: ClockProvider = new ClockProvider {
      override val defaultClock: Clock = {
        val fixedInstant = LocalDateTime.parse(frozenTimeString).toInstant(UTC)
        Clock.fixed(fixedInstant, ZoneId.systemDefault)
      }
    }
  }

  implicit lazy val webDriver: HtmlUnitDriver = {
    val wd = new HtmlUnitDriver(true)
    wd.setJavascriptEnabled(false)
    wd
  }

  lazy val mongo = app.injector.instanceOf[MongoComponent]

  def dropMongo(): Unit = {
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    mongo.database.drop().toFuture().map(_ => ()).futureValue

  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    webDriver.manage().deleteAllCookies()
    dropMongo()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    dropMongo()
  }

  override implicit protected lazy val runningServer: RunningServer =
    TestServerFactory.start(app)

  object TestServerFactory extends DefaultTestServerFactory {
    override protected def serverConfig(app: Application): ServerConfig = {
      val sc = ServerConfig(port    = Some(testPort), sslPort = Some(0), mode = Mode.Test, rootDir = app.path)
      sc.copy(configuration = sc.configuration.withFallback(overrideServerConfiguration(app)))
    }
  }

  lazy val startPage: StartPage = wire[StartPage]
  lazy val ggSignInPage: GgSignInPage = wire[GgSignInPage]
  lazy val taxLiabilitiesPage: CalculatorTaxLiabilitiesPage = wire[CalculatorTaxLiabilitiesPage]
  lazy val paymentTodayQuestionPage: PaymentTodayQuestionPage = wire[PaymentTodayQuestionPage]
  lazy val paymentTodayCalculatorPage: PaymentTodayCalculatorPage = wire[PaymentTodayCalculatorPage]
  lazy val paymentSummaryPage: PaymentSummaryPage = wire[PaymentSummaryPage]
  lazy val startAffordabilityPage: StartAffordabilityPage = wire[StartAffordabilityPage]
  lazy val addIncomeSpendingPage: AddIncomeSpendingPage = wire[AddIncomeSpendingPage]
  lazy val callUsNoIncomePage: CallUsNoIncomePage = wire[CallUsNoIncomePage]
  lazy val yourMonthlySpendingPage: YourMonthlySpendingPage = wire[YourMonthlySpendingPage]
  lazy val howMuchYouCouldAffordPage: HowMuchYouCouldAffordPage = wire[HowMuchYouCouldAffordPage]
  lazy val weCannotAgreeYourPaymentPlanPage: WeCannotAgreeYourPaymentPlanPage = wire[WeCannotAgreeYourPaymentPlanPage]
  lazy val howMuchCanYouPayEachMonthPage: HowMuchCanYouPayEachMonthPage = wire[HowMuchCanYouPayEachMonthPage]
  lazy val selectDatePage: InstalmentSummarySelectDatePage = wire[InstalmentSummarySelectDatePage]
  lazy val checkYourPaymentPlanPage: CheckYourPaymentPlanPageForPaymentDay28thOfMonth =
    wire[CheckYourPaymentPlanPageForPaymentDay28thOfMonth]
  lazy val checkYourPaymentPlanPageForPayment11thOfMonth: CheckYourPaymentPlanPageForPaymentDay11thOfMonth =
    wire[CheckYourPaymentPlanPageForPaymentDay11thOfMonth]
  lazy val aboutBankAccountPage: AboutBankAccountPage = wire[AboutBankAccountPage]
  lazy val termsAndConditionsPage: TermsAndConditionsPage = wire[TermsAndConditionsPage]
  lazy val directDebitPage: DirectDebitPage = wire[DirectDebitPage]
  lazy val directDebitConfirmationPage: DirectDebitConfirmationPage = wire[DirectDebitConfirmationPage]
  lazy val arrangementSummaryPage: ArrangementSummaryPage = wire[ArrangementSummaryPage]
  lazy val viewPaymentPlanPage: ViewPaymentPlanPage = wire[ViewPaymentPlanPage]
  lazy val yourMonthlyIncomePage: YourMonthlyIncomePage = wire[YourMonthlyIncomePage]
  lazy val directDebitErrorPage: DirectDebitErrorPage = wire[DirectDebitErrorPage]

  // not eligible pages
  lazy val debtTooLargePage: DebtTooLargePage = wire[DebtTooLargePage]
  lazy val notOnIaPage: NotOnIaPage = wire[NotOnIaPage]
  lazy val generalCallUsPage: GeneralCallUsPage = wire[GeneralCallUsPage]
  lazy val callUsDebtTooOld: CallUsDebtTooOldPage = wire[CallUsDebtTooOldPage]
  lazy val needToFilePage: FileYourTaxReturnPage = wire[FileYourTaxReturnPage]
  lazy val alreadyHaveAPlanPage: AlreadyHaveAPlanPage = wire[AlreadyHaveAPlanPage]
  lazy val notEnrolledPage: NotEnrolledPage = wire[NotEnrolledPage]
  lazy val youNeedToRequestAccessToSelfAssessment: YouNeedToRequestAccessToSelfAssessmentPage = wire[YouNeedToRequestAccessToSelfAssessmentPage]
  lazy val enrolForSaPage: EnrolForSaPage = wire[EnrolForSaPage]
  lazy val notSoleSignatoryPage: NotSoleSignatoryPage = wire[NotSoleSignatoryPage]
  lazy val setUpPlanWithAdviserPage: SetUpPlanWithAdviserPage = wire[SetUpPlanWithAdviserPage]
}
