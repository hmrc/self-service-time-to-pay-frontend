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

package sessioncache

import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

//TODO: remove it https://jira.tools.tax.service.gov.uk/browse/OPS-2713
class SsttpSessionCache @Inject() (servicesConfig: ServicesConfig, httpClient: HttpClient) extends uk.gov.hmrc.http.cache.client.SessionCache {
  override def defaultSource: String = servicesConfig.getString("appName")

  override def baseUri: String = servicesConfig.baseUrl("keystore")

  override def domain: String = "keystore" //??

  override def http = httpClient
}

