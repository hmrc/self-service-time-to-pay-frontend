/*
 * Copyright 2017 HM Revenue & Customs
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

import scala.util.Try

object DirectDebitForm {
  val minAccountNumber = 0
  val maxAccountNumber = 999999999

  def parseIntOption(str:String) =  Try(str.toInt).toOption

  val directDebitMapping = mapping(
    "accountName" -> text.verifying("ssttp.direct-debit.form.error.accountName.required", _.trim!="")
      .verifying("ssttp.direct-debit.form.error.accountName.not-text", x => x.length == x.replaceAll("[^a-zA-Z '.& \\/]", "").length),
    "sortCode" -> text.verifying("ssttp.direct-debit.form.error.sortCode.required", _.trim!="")
      .verifying("ssttp.direct-debit.form.error.sortCode.not-number", x => (x.trim=="") || (x.replaceAll("[^0-9]", "")!="") || parseIntOption(x).nonEmpty)
      .verifying("ssttp.direct-debit.form.error.sortCode.not-valid", x=> (x.replaceAll("[^0-9]", "").length == 6) && x.length == 6),
    "accountNumber" -> text.verifying("ssttp.direct-debit.form.error.accountNumber.required", _.trim!="")
      .verifying("ssttp.direct-debit.form.error.accountNumber.not-valid", x => (x.replaceAll("[^0-9]", "").length == 8) && x.length == 8)
  )(ArrangementDirectDebit.apply)(ArrangementDirectDebit.unapply)

  val directDebitForm = Form(directDebitMapping)

  val existingBankAccountForm = Form(mapping(
    "existingDdi" -> optional(text),
    "directDebit" -> optional(directDebitMapping)
  )(ArrangementExistingDirectDebit.apply)(ArrangementExistingDirectDebit.unapply))
}
