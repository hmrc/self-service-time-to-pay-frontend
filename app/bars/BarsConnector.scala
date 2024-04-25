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

package bars

import bars.model._
import com.google.inject._
import play.api.libs.json.Json
import play.api.mvc.Request
import ssttparrangement.SubmissionError
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.selfservicetimetopay.models._
import _root_.util.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Bank Account Reputation (Bars) Connector
 */
@Singleton
class BarsConnector @Inject() (
    servicesConfig: ServicesConfig,
    httpClient:     HttpClient)(
    implicit
    ec: ExecutionContext
) extends Logging {

  type DDSubmissionResult = Either[SubmissionError, DirectDebitInstructionPaymentPlan]

  import req.RequestSupport._

  val baseUrl: String = servicesConfig.baseUrl("bars")

  /**
   * Checks if the given bank details are valid
   * See https://github.com/hmrc/bank-account-reputation/blob/master/docs/eiscd/v2/validateBankDetails.md
   * and more importantly
   * https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=TEC&postingDay=2020%2F11%2F6&title=Bank-account-reputation+will+return+a+bad-request+for+HMRC+sort+codes
   */
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def validateBank(validateBankAccountRequest: ValidateBankDetailsRequest)(implicit request: Request[_]): Future[BarsResponse] = {
    val url = s"$baseUrl/validate/bank-details"
    httpClient.POST[ValidateBankDetailsRequest, HttpResponse](url, validateBankAccountRequest)
      .map {
        case r: HttpResponse if r.status == 200 => BarsResponseOk(Json.parse(r.body).as[ValidateBankDetailsResponse])
        case r: HttpResponse =>

          HttpReads.handleResponseEither("POST", url)(r) match {
            case Right(_) =>
              val barsError = Json.parse(r.body).as[BarsError]

              if (barsError.code == BarsError.sortCodeOnDenyList) {
                BarsResponseSortCodeOnDenyList(barsError)
              } else {
                throw new RuntimeException(s"Unhandled error code for ${r.status} HttpResponse: [$barsError]")
              }
            case Left(upstreamErrorResponse) =>
              throw upstreamErrorResponse
          }

      }
  }

}

