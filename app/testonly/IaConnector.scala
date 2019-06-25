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

package testonly

import com.google.inject.Singleton
import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IaConnector @Inject() (
    httpClient:     HttpClient,
    servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext) {

  private lazy val baseUrl: String = servicesConfig.baseUrl("ia")

  def uploadUtr(utr: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .POSTEmpty(baseUrl + s"/ia/upload/$utr")
      .map{
        r =>
          if (r.status != 200) throw new RuntimeException(s"Could not upload utr into Ia")
      }
}
