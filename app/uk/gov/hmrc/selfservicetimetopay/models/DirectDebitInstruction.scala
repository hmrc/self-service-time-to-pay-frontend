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

import java.time.LocalDate

//Direct-debit - getBanks response
//Direct-debit - part of input to createPaymentPlan
case class DirectDebitInstruction(sortCode:        Option[String]    = None,
                                  accountNumber:   Option[String]    = None,
                                  referenceNumber: Option[String]    = None,
                                  creationDate:    Option[LocalDate] = None,
                                  paperAuddisFlag: Option[Boolean]   = Some(true),
                                  ddiRefNumber:    Option[String]    = None,
                                  ddiReferenceNo:  Option[String]    = None,
                                  accountName:     Option[String]    = None) {

  def obfuscate: DirectDebitInstruction = DirectDebitInstruction(
    sortCode        = sortCode.map(_ => "***"),
    accountNumber   = accountNumber.map(_ => "***"),
    referenceNumber = referenceNumber.map(_ => "***"),
    creationDate    = creationDate,
    paperAuddisFlag = paperAuddisFlag,
    ddiRefNumber    = ddiRefNumber.map(_ => "***"),
    ddiReferenceNo  = ddiReferenceNo.map(_ => "***"),
    accountName     = accountName.map(_ => "***")
  )
}
