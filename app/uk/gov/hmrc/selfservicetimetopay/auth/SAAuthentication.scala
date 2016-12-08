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

package uk.gov.hmrc.selfservicetimetopay.auth

import play.api.Logger
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthenticationProvider, GovernmentGateway, TaxRegime}
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig
import uk.gov.hmrc.selfservicetimetopay.controllers.routes


object SaGovernmentGateway extends GovernmentGateway {
  override def continueURL: String = s"${SsttpFrontendConfig.loginCallbackBaseUrl}/pay-what-you-owe-in-instalments/arrangement/determine-misalignment"

  override def loginURL: String = SsttpFrontendConfig.loginUrl

  override def sessionKeysToKeep = Seq(SsttpFrontendConfig.ttpSessionId)
}

object SaRegime extends TaxRegime {
  override def isAuthorised(accounts: Accounts): Boolean = accounts.sa.isDefined

  override def authenticationType: AuthenticationProvider = SaGovernmentGateway

  override def unauthorisedLandingPage = {
    Logger.info("No SA enrolment for current user")
    Some(routes.SelfServiceTimeToPayController.getTtpCallUs().url)
  }
}
