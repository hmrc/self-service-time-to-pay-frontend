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

package uk.gov.hmrc.selfservicetimetopay.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.selfservicetimetopay.models.{ArrangementDirectDebit, ArrangementExistingDirectDebit}

object DirectDebitForm {
  val minAccountNumber = 0
  val maxAccountNumber = 999999999

  val directDebitMapping = mapping(
    "accountName" -> nonEmptyText,
    "sortCode" -> nonEmptyText,
    "accountNumber" -> nonEmptyText
  )(ArrangementDirectDebit.apply)(ArrangementDirectDebit.unapply)

  val directDebitForm = Form(directDebitMapping)

  val existingBankAccountForm = Form(mapping(
    "existingDdi" -> optional(text),
    "directDebit" -> optional(directDebitMapping)
  )(ArrangementExistingDirectDebit.apply)(ArrangementExistingDirectDebit.unapply))
}
