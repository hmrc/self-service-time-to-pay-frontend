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

package bars.model

import org.apache.commons.lang3.StringUtils
import play.api.libs.json.{Format, Json}

final case class Account(
    sortCode:      String,
    accountNumber: String
) {

  def obfuscate: Account = Account(
    sortCode      = "***",
    accountNumber = "***"
  )

  override def toString: String = {
    obfuscate.productIterator.mkString(productPrefix + "(", ",", ")")
  }
}

object Account {

  def padded(sortCode: String, accountNumber: String): Account =
    Account(sortCode, leftPad(accountNumber))

  private val minimumLength = 8
  private val padStr = "0"

  private def leftPad(accountNumber: String): String =
    StringUtils.leftPad(accountNumber, minimumLength, padStr)

  implicit val format: Format[Account] = Json.format[Account]
}
