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

import play.api.libs.json.{Format, Json}

final case class ArrangementDirectDebit(accountName: String, sortCode: String, accountNumber: String) {
  def formatSortCode: String = sortCode.grouped(2).foldLeft("")((subset, total) => subset + " - " + total).drop(3)
}

object ArrangementDirectDebit {

  def cleanSortCode(sortCode: String): String = sortCode
    .replaceAll("-", "")
    .replaceAll(" ", "")
    .trim

  def cleanAccountNumber(accountNumber: String): String = accountNumber.replaceAll(" ", "").trim

  def from(bankDetails: BankDetails): ArrangementDirectDebit =
    ArrangementDirectDebit(bankDetails.accountName, cleanSortCode(bankDetails.sortCode), cleanAccountNumber(bankDetails.accountNumber))

  def to(arrangementDirectDebit: ArrangementDirectDebit): Option[BankDetails] =
    Some(BankDetails(accountName   = arrangementDirectDebit.accountName,
                     sortCode      = arrangementDirectDebit.sortCode,
                     accountNumber = arrangementDirectDebit.accountNumber))

  implicit val format: Format[ArrangementDirectDebit] = Json.format[ArrangementDirectDebit]

}
