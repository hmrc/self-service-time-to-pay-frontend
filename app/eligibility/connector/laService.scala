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

package eligibility.connector

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class IaService @Inject() (http: HttpClient, config: IaConfig) {
  val iaUrl: String = config.baseUrl
  val enableCheck = config.enableIACheck

  def checkIaUtr(utr: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    if (enableCheck) {
      http.GET(iaUrl + s"/ia/$utr").map(res => res.status match {
        case 200 => true
        case 204 => false
      })
    } else {
      Future.successful(true)
    }
  }
}

