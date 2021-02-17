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

package bars.model

import play.api.libs.json.{Json, OFormat}

final case class ValidateBankDetailsResponse(
    accountNumberWithSortCodeIsValid:         String,
    nonStandardAccountDetailsRequiredForBacs: String,
    sortCodeIsPresentOnEISCD:                 String,
    supportsBACS:                             Option[String],
    ddiVoucherFlag:                           Option[String],
    directDebitsDisallowed:                   Option[String],
    directDebitInstructionsDisallowed:        Option[String],
    iban:                                     Option[String],
    sortCodeBankName:                         Option[String]
) {

  def obfuscate: ValidateBankDetailsResponse = this.copy(
    iban = iban.map(_ => "***")
  )

  val isModCheckValid: Option[Boolean] =
    if (accountNumberWithSortCodeIsValid == "yes") Some(true)
    else if (accountNumberWithSortCodeIsValid == "no") Some(false)
    else None

  val isBacsSupported: Option[Boolean] = supportsBACS.map(_ == "yes")

  val isDdSupported: Option[Boolean] = {
    val isDdAllowed: Option[Boolean] = directDebitsDisallowed.map(_ == "no")
    val isDdInstructionAllowed: Option[Boolean] = directDebitInstructionsDisallowed.map(_ == "no")

    //returns true if there is any evidence that dd is supported
    (isDdAllowed, isDdInstructionAllowed) match {
      case (Some(true), Some(true))   => Some(true)
      case (Some(true), Some(false))  => Some(true)
      case (Some(false), Some(true))  => Some(true)
      case (Some(false), Some(false)) => Some(false)
      case (None, Some(x))            => Some(x)
      case (Some(x), None)            => Some(x)
      case (None, None)               => None
    }
  }

  val isValid: Boolean = {
    isDdSupported match {
      case Some(true)  => true
      case Some(false) => false
      case None => isBacsSupported match {
        case Some(true)  => true
        case Some(false) => false
        case None => isModCheckValid match {
          case Some(true)  => true
          case Some(false) => false
          case None        => false
        }
      }
    }
  }
}

object ValidateBankDetailsResponse {
  implicit val format: OFormat[ValidateBankDetailsResponse] = Json.format[ValidateBankDetailsResponse]

}
