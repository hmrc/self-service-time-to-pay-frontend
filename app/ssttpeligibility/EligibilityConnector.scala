/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityRequest, EligibilityStatus}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.{ExecutionContext, Future}

class EligibilityConnector @Inject() (
    httpClient:     HttpClient,
    servicesConfig: ServicesConfig)(
    implicit
    ec: ExecutionContext
) {

  import req.RequestSupport._

  val baseUrl: String = servicesConfig.baseUrl("time-to-pay-eligibility")

  def checkEligibility(eligibilityRequest: EligibilityRequest, utr: SaUtr)(implicit request: Request[_]): Future[EligibilityStatus] = {
    httpClient.POST[EligibilityRequest, EligibilityStatus](s"$baseUrl/time-to-pay-eligibility/eligibility/${utr.value}", eligibilityRequest)
  }
}
