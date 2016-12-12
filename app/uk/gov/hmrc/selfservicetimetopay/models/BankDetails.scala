/*
 * Copyright 2016 HM Revenue & Customs
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

case class BankDetails(sortCode: String,
                       accountNumber: String,
                       bankName: Option[String] = None,
                       bankAddress: Option[Address] = None,
                       accountName: Option[String] = None,
                       ddiRefNumber: Option[String] = None) {

  def formattedSortCode():String = {
    "%s - %s - %s".format(sortCode.substring(0, 1), sortCode.substring(2, 3), sortCode.substring(4, 5))
  }
}
