/*
 * Copyright 2017 HM Revenue & Customs
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

import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityRequest, EligibilityStatus, SelfAssessment}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import com.google.inject._

import scala.concurrent.Future

@ImplementedBy(classOf[EligibilityConnectorImpl])
trait EligibilityConnector {

  val eligibilityURL: String
  val serviceURL: String
  val http: HttpPost

  def checkEligibility(eligibilityRequest: EligibilityRequest)(implicit hc: HeaderCarrier): Future[EligibilityStatus] = {
    http.POST[EligibilityRequest, EligibilityStatus](s"$eligibilityURL/$serviceURL", eligibilityRequest)
  }
}

@Singleton
class EligibilityConnectorImpl extends EligibilityConnector with ServicesConfig {
  val eligibilityURL: String = baseUrl("time-to-pay-eligibility")
  val serviceURL = "time-to-pay-eligibility/eligibility"
  val http = WSHttp
}
