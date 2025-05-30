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

package enrolforsa

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Request
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddTaxesConnector @Inject() (
    servicesConfig: ServicesConfig,
    httpClient:     HttpClientV2)(
    implicit
    ec: ExecutionContext
) {

  import req.RequestSupport._
  private val baseUrl: String = servicesConfig.baseUrl("add-taxes")

  def startEnrolForSaJourney(maybeSaUtr: Option[SaUtr], credentials: Credentials)(implicit request: Request[_]): Future[StartEnrolForSaJourneyResult] = {
    val url = s"$baseUrl/internal/self-assessment/enrol-for-sa"

    val body = Json.obj(
      "origin" -> "ssttp-sa",
      "providerId" -> credentials.providerId
    ) ++ maybeSaUtr.fold(Json.obj())(utr => Json.obj("utr" -> utr))

    httpClient.post(url"$url")
      .withBody(Json.toJson(body))
      .execute[StartEnrolForSaJourneyResult]
  }
}

final case class StartEnrolForSaJourneyResult(
    redirectUrl: String
)

object StartEnrolForSaJourneyResult {
  implicit val format: OFormat[StartEnrolForSaJourneyResult] = Json.format[StartEnrolForSaJourneyResult]
}

