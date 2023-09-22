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

package uk.gov.hmrc.selfservicetimetopay.models

import crypto.model.{Encryptable, Encrypted}
import model.enumsforforms.TypeOfBankAccount
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

final case class BankDetails(
    typeOfAccount:     Option[TypeOfBankAccount] = None,
    sortCode:          String,
    accountNumber:     String,
    accountName:       String,
    maybeDDIRefNumber: Option[String]            = None) extends Encryptable[BankDetails] {

  def obfuscate: BankDetails = BankDetails(
    typeOfAccount     = typeOfAccount,
    sortCode          = "***",
    accountNumber     = "***",
    accountName       = "***",
    maybeDDIRefNumber = maybeDDIRefNumber.map(_ => "***")
  )

  override def toString: String = {
    obfuscate.productIterator.mkString(productPrefix + "(", ",", ")")
  }

  override def encrypt: EncryptedBankDetails = EncryptedBankDetails(
    typeOfAccount,
    SensitiveString(sortCode),
    SensitiveString(accountNumber),
    SensitiveString(accountName),
    maybeDDIRefNumber
  )
}

object BankDetails {
  implicit val format: Format[BankDetails] = Json.format[BankDetails]

}

case class EncryptedBankDetails(
                                 typeOfAccount:     Option[TypeOfBankAccount] = None,
                                 sortCode:          SensitiveString,
                                 accountNumber:     SensitiveString,
                                 accountName:       SensitiveString,
                                 maybeDDIRefNumber: Option[String]            = None) extends Encrypted[BankDetails] {
  override def decrypt: BankDetails = BankDetails(
    typeOfAccount,
    sortCode.decryptedValue,
    accountNumber.decryptedValue,
    accountName.decryptedValue,
    maybeDDIRefNumber
  )
}

object EncryptedBankDetails {
  implicit val format: OFormat[EncryptedBankDetails] = Json.format[EncryptedBankDetails]
}
