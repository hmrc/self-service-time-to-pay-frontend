/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.ssttp.connectors

import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.ssttp.config.WSHttp

object SSTTPConnector extends ServicesConfig {

  val http: HttpGet with HttpPost = WSHttp

  /* def totalLiability(isEligible : Boolean) (implicit hc : HeaderCarrier) : Future[LiabilityResult] = {
     Logger.info("Calling the GET Liability - :)")
     http.GET[LiabilityResult](url(s"/ssttp/total-liability?eligible=" + isEligible))
   }
 */
  private def url(path: String) = baseUrl("self-service-time-to-pay") + path
}

