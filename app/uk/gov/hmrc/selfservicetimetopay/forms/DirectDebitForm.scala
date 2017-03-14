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

import play.api.data.Forms._
import play.api.data.{Form, FormError}
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDirectDebit

import scala.util.Try

object DirectDebitForm {
  val minAccountNumber = 0
  val maxAccountNumber = 999999999

  def parseIntOption(str: String) = Try(str.toInt).toOption

  val directDebitMapping = mapping(
    "accountName" -> text.verifying("ssttp.direct-debit.form.error.accountName.required", _.trim != "")
      .verifying("ssttp.direct-debit.form.error.accountName.not-text", x => x.length == x.replaceAll("[^a-zA-Z '.& \\/]", "").length),
    "sortCode1" -> text.verifying("ssttp.direct-debit.form.error.sortCode.required", _.trim != "")
      .verifying("ssttp.direct-debit.form.error.sortCode.not-number", x => validateSortCode(x))
      .verifying("ssttp.direct-debit.form.error.sortCode.not-valid", x => validateNumberLength(x, 2)),
    "sortCode2" -> text.verifying("ssttp.direct-debit.form.error.sortCode.required", _.trim != "")
      .verifying("ssttp.direct-debit.form.error.sortCode.not-number", x => validateSortCode(x))
      .verifying("ssttp.direct-debit.form.error.sortCode.not-valid", x => validateNumberLength(x, 2)),
    "sortCode3" -> text.verifying("ssttp.direct-debit.form.error.sortCode.required", _.trim != "")
      .verifying("ssttp.direct-debit.form.error.sortCode.not-number", x => validateSortCode(x))
      .verifying("ssttp.direct-debit.form.error.sortCode.not-valid", x => validateNumberLength(x, 2)),
    "accountNumber" -> text.verifying("ssttp.direct-debit.form.error.accountNumber.required", _.trim != "")
      .verifying("ssttp.direct-debit.form.error.accountNumber.not-valid", x => validateNumberLength(x, 8))
  )({ case (name, sc1, sc2, sc3, acctNo) => ArrangementDirectDebit(name, sc1 ++ sc2 ++ sc3, acctNo) }
  )({ case arrangementDirectDebit =>
    val (sc1::sc2::sc3::_)= arrangementDirectDebit.sortCode.grouped(2).toList
    Some((arrangementDirectDebit.accountName,
      sc1,
      sc2,
      sc3,
      arrangementDirectDebit.accountNumber))
  })

  def validateSortCode(sortCodeBit: String): Boolean = {
    (sortCodeBit.trim == "") || (sortCodeBit.replaceAll("[^0-9]", "") != "") || parseIntOption(sortCodeBit).nonEmpty
  }

  def validateNumberLength(number: String, length: Int): Boolean = {
    number.replaceAll("[^0-9]", "").length == length
  }

  val directDebitForm = Form(directDebitMapping)
  val directDebitFormWithBankAccountError = directDebitForm.copy(errors = Seq(FormError(" ", "ssttp.direct-debit.form.bank-not-found-info")))

}
