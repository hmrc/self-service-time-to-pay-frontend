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

package ssttpeligibility

import com.google.inject._
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityRequest, EligibilityStatus}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.{ExecutionContext, Future}

class EligibilityConnector @Inject() (httpClient: HttpClient, servicesConfig: ServicesConfig) {

  import req.RequestSupport._

  val eligibilityURL: String = "time-to-pay-eligibility/eligibility"
  val serviceURL: String = servicesConfig.baseUrl("time-to-pay-eligibility")

  def checkEligibility(eligibilityRequest: EligibilityRequest, utr: String)(implicit request: Request[_], ec: ExecutionContext): Future[EligibilityStatus] = {
    httpClient.POST[EligibilityRequest, EligibilityStatus](s"$eligibilityURL/$serviceURL/$utr", eligibilityRequest)
  }
}
