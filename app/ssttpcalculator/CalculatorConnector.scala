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

package ssttpcalculator

import com.google.inject._
import play.api.mvc.Request
import timetopaycalculator.cor.model.{CalculatorInput, PaymentSchedule}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class CalculatorConnector @Inject() (servicesConfig: ServicesConfig, httpClient: HttpClient) {

  import req.RequestSupport._

  val baseUrl: String = servicesConfig.baseUrl("time-to-pay-calculator")
  /**
   * Send the calculator input information to the time-to-pay-calculator service and retrieve back a payment schedule
   */
  def calculatePaymentSchedule(calcInput: CalculatorInput)(implicit request: Request[_]): Future[PaymentSchedule] = {
    httpClient
      .POST[CalculatorInput, PaymentSchedule](
        s"$baseUrl/time-to-pay-calculator/paymentschedule",
        calcInput
      )
  }
}
