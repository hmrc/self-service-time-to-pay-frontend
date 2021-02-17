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

import bars.model.{Account, ValidateBankDetailsRequest, ValidateBankDetailsResponse}
import javax.inject.Inject
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}

sealed trait BarsValidationResult

final case class Valid(obfuscatedResponse: ValidateBankDetailsResponse) extends BarsValidationResult

final case class Invalid(obfuscatedResponse: ValidateBankDetailsResponse) extends BarsValidationResult

class BarsService @Inject() (barsConnector: BarsConnector)(implicit ec: ExecutionContext) {

  def validateBankDetails(sortCode: String, accountNumber: String)(implicit request: Request[_]): Future[BarsValidationResult] = {
    val validateBankDetailsRequest = ValidateBankDetailsRequest(account = Account(
      sortCode,
      accountNumber
    ))

    for {
      response <- barsConnector.validateBank(validateBankDetailsRequest)
      result = if (response.isValid) Valid(response) else Invalid(response)
    } yield result
  }

}

