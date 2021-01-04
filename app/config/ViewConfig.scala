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

package config

import com.google.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

final case class ViewConfig(
    appName:        String,
    assetsPrefix:   String,
    analyticsToken: String,
    analyticsHost:  String,
    authUrl:        String,
    companyAuthUrl: String,
    signInPath:     String,
    //    signOut:              String,
    //    frontendBaseUrl:      String,
    frontendBaseUrl: String
) {

  //  val reportAProblemPartialUrl = s"$frontendBaseUrl/contact/problem_reports_ajax?service=$appName"
  //  val reportAProblemNonJSUrl = s"$frontendBaseUrl/contact/problem_reports_nonjs?service=$appName"

  val loginUrl = companyAuthUrl + signInPath

  @Inject
  def this(servicesConfig: ServicesConfig) = this(
    appName        = servicesConfig.getString("appName"),
    assetsPrefix   = servicesConfig.getString(s"assets.url") + servicesConfig.getString(s"assets.version"),
    analyticsToken = servicesConfig.getString(s"google-analytics.token"),
    analyticsHost  = servicesConfig.getString(s"google-analytics.host"),
    authUrl        = servicesConfig.baseUrl("auth"),
    companyAuthUrl = servicesConfig.getString("microservice.services.company-auth.url"),
    signInPath     = servicesConfig.getString("microservice.services.company-auth.sign-in-path"),
    //    signOut              = servicesConfig.getString("urls.logout"),
    //    frontendBaseUrl      = servicesConfig.getString("frontend-base-url"),
    frontendBaseUrl = servicesConfig.getString("microservice.services.auth.login-callback.base-url") //TODO: migrate this config
  )

}
