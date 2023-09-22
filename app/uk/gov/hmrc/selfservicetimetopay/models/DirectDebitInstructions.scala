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
import play.api.libs.json.{Format, Json, OFormat}

final case class DirectDebitInstructions(directDebitInstruction: Seq[DirectDebitInstruction]) extends Encryptable[DirectDebitInstructions] {
  def obfuscate: DirectDebitInstructions = DirectDebitInstructions(
    directDebitInstruction = directDebitInstruction.map(_.obfuscate)
  )

  override def toString: String = {
    obfuscate.productIterator.mkString(productPrefix + "(", ",", ")")
  }

  override def encrypt: Encrypted[DirectDebitInstructions] = EncryptedDirectDebitInstructions(
    directDebitInstruction
  )
}

object DirectDebitInstructions {
  implicit val format: Format[DirectDebitInstructions] = Json.format[DirectDebitInstructions]
}

case class EncryptedDirectDebitInstructions(
                                             directDebitInstruction: Seq[DirectDebitInstruction]
                                           ) extends Encrypted[DirectDebitInstructions] {
  override def decrypt: DirectDebitInstructions = DirectDebitInstructions(
    directDebitInstruction
  )
}

object EncryptedDirectDebitInstructions {
  implicit val format: OFormat[EncryptedDirectDebitInstructions] = Json.format[EncryptedDirectDebitInstructions]

}
