/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class IaService @Inject() (http:           HttpClient,
                           servicesConfig: ServicesConfig) {
  private lazy val baseUrl: String = servicesConfig.baseUrl("ia")

  val enableCheck = false

  // IA is not turned on -> everyone is allowed to use the service, always return true until we need to call the ia microservice
  def checkIaUtr(utr: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    if (enableCheck) {
      http.GET[HttpResponse](baseUrl + s"/ia/$utr").map(res => res.status match {
        case 200 => true
        case 204 => false
      })
    } else {
      Future.successful(true)
    }
  }
}
