/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.connectors

import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import com.google.inject._

/**
  * This is used only by test-only endpoint.
  */
@ImplementedBy(classOf[AuthLoginApiConnectorImpl])
trait AuthLoginApiConnector {
  val calculatorURL: String
  val serviceURL: String
  val http: HttpPost

  def calculatePaymentSchedule(liabilities: CalculatorInput)(implicit hc: HeaderCarrier): Future[Seq[CalculatorPaymentSchedule]] = {
    http.POST[CalculatorInput, Seq[CalculatorPaymentSchedule]](s"$calculatorURL/$serviceURL", liabilities)
  }
}

@Singleton
class AuthLoginApiConnectorImpl extends CalculatorConnector with ServicesConfig {
  val calculatorURL: String = baseUrl("time-to-pay-calculator")
  val serviceURL = "time-to-pay-calculator/paymentschedule"
  val http = WSHttp
}
