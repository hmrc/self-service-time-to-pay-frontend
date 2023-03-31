/*
 * Copyright 2023 HM Revenue & Customs
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

package pagespecs

import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs.{AuthStub, GgStub, IaStub, TaxpayerStub}
import uk.gov.hmrc.selfservicetimetopay.models.TotalDebtIsTooHigh

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances}

class StartPageUnauthSpec extends ItSpec {
  override val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

      val retrievalResult = Future.successful(
        new ~(new ~(Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "")), "Activated", None))), None), None)

      //      new~(new~(Enrolments(Set.empty[Enrolment]), None), None)
      )
      (retrievalResult.map(_.asInstanceOf[A]))
    }
  }

  "unauthorised - missing bearer token (user not logged in)" ignore {
    AuthStub.unathorisedMissingSession()
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    ggSignInPage.assertInitialPageIsDisplayed
  }

}
