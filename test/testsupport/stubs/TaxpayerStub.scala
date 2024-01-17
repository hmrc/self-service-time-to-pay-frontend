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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPathPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.libs.json.Json.{prettyPrint, toJson}
import testsupport.testdata.EligibilityTaxpayerVariationsTd._
import testsupport.testdata.TdAll.{taxpayer, taxpayerNddsRejects, utr}
import timetopaytaxpayer.cor.model.Taxpayer
import uk.gov.hmrc.selfservicetimetopay.models._

object TaxpayerStub extends Matchers with Status {
  private val url: UrlPathPattern = urlPathEqualTo(s"/taxpayer/$utr")

  def getTaxpayer(tp: Taxpayer): StubMapping =
    stubFor(get(url).willReturn(aResponse().withStatus(OK).withBody(prettyPrint(toJson(tp)))))

  def getTaxpayer(): StubMapping =
    getTaxpayer(taxpayer)

  def getTaxpayerNddsRejects(): StubMapping =
    getTaxpayer(taxpayerNddsRejects)

  def getTaxpayer(reason: Reason): StubMapping =
    stubFor(get(url).willReturn(aResponse()
      .withStatus(OK)
      .withBody(prettyPrint(toJson(getIneligibleTaxpayerModel(reason))))))

  def taxpayerNotFound(): StubMapping =
    stubFor(get(url).willReturn(aResponse().withStatus(NOT_FOUND)))

}
