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

import audit.AuditService
import bars.model._
import play.api.Logging
import play.api.mvc.Request
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.selfservicetimetopay.models.TypeOfAccountDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BarsService @Inject() (barsConnector: BarsConnector, auditService: AuditService)(implicit ec: ExecutionContext) extends Logging {

  def validateBankDetails(sortCode:                  String,
                          accountNumber:             String,
                          accountName:               String,
                          maybeTypeOfAccountDetails: Option[TypeOfAccountDetails],
                          saUtr:                     SaUtr
  )(implicit request: Request[_]): Future[BarsValidationResult] = {

    val validateBankDetailsRequest = ValidateBankDetailsRequest(account = Account.padded(
      sortCode,
      accountNumber
    ))

    for {
      barsResponse <- barsConnector.validateBank(validateBankDetailsRequest)
      barsValidationResult = barsResponse match {
        case barsResponse @ BarsResponseOk(validateBankDetailsResponse) =>
          auditService.sendBarsValidateEvent(sortCode, accountNumber, accountName, maybeTypeOfAccountDetails, saUtr, validateBankDetailsResponse)
          logger.debug(s"BARs response: $barsResponse")
          val obfuscatedBarsResponse = barsResponse.copy(validateBankDetailsResponse.obfuscate)
          if (validateBankDetailsResponse.isValid) ValidBankDetails(obfuscatedBarsResponse) else InvalidBankDetails(obfuscatedBarsResponse)
        case barsResponse: BarsResponseSortCodeOnDenyList =>
          logger.debug(s"BARs response: $barsResponse")
          InvalidBankDetails(barsResponse)
      }
    } yield barsValidationResult
  }

}

