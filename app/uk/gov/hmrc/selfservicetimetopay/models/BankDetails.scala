/*
 * Copyright 2020 HM Revenue & Customs
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
import timetopaytaxpayer.cor.model.Address

case class BankDetails(sortCode:      Option[String]  = None,
                       accountNumber: Option[String]  = None,
                       bankName:      Option[String]  = None,
                       bankAddress:   Option[Address] = None,
                       accountName:   Option[String]  = None,
                       ddiRefNumber:  Option[String]  = None) {

  def obfuscate: BankDetails = BankDetails(
    sortCode      = sortCode.map(_ => "***"),
    accountNumber = accountNumber.map(_ => "***"),
    bankName      = bankName.map(_ => "***"),
    bankAddress   = bankAddress.map(_.obfuscate),
    accountName   = accountName.map(_ => "***"),
    ddiRefNumber  = ddiRefNumber.map(_ => "***")
  )
}

object BankDetails {
  implicit val format: Format[BankDetails] = Json.format[BankDetails]
}
