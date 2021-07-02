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

package bars

import bars.model._
import com.google.inject._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import ssttparrangement.SubmissionError
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Bank Account Reputation (Bars) Connector
 */
class BarsConnector @Inject() (
    servicesConfig: ServicesConfig,
    httpClient:     HttpClient)(
    implicit
    ec: ExecutionContext
) {
  private val logger = Logger(getClass)

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
    val url = s"$baseUrl/v2/validateBankDetails"
    httpClient.POST[ValidateBankDetailsRequest, HttpResponse](url, validateBankAccountRequest)(implicitly, rds = httpResponseReads, implicitly, implicitly)
      .map {
        case r: HttpResponse if r.status == 200 => BarsResponseOk(Json.parse(r.body).as[ValidateBankDetailsResponse])
        case r: HttpResponse if r.status == 400 =>
          val barsError = Json.parse(r.body).as[BarsError]
          if (barsError.code == BarsError.sortCodeOnDenyList) {
            BarsResponseSortCodeOnDenyList(barsError)
          } else {
            logger.error(s"Unhandled error code for 400 HttpResponse: [$barsError]")
            HttpReads.handleResponse("POST", url)(r)

            //any other idea? Above will throw exception. We can't refactor it because it's a lib and we
            // want to preserve current http verbs way of handling it
            null: BarsResponse
          }
      }
  }

  //TODO this will be gone once we migrate to newer HttpVerbs
  implicit val httpResponseReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = response
  }

}

