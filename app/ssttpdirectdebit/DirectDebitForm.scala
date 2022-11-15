/*
 * Copyright 2022 HM Revenue & Customs
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

package ssttpdirectdebit

import play.api.data.Forms._
import play.api.data.{Form, FormError}
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDirectDebit
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDirectDebit.cleanSortCode

import scala.util.Try

object DirectDebitForm {

  def condTrue(condition: Boolean, statement: Boolean) = if (condition) statement else true

  val directDebitMapping = mapping(
    "accountName" -> text.verifying("ssttp.direct-debit.form.error.accountName.required", _.trim != "")
      .verifying("ssttp.direct-debit.form.error.accountName.check", x => condTrue(x.trim != "", x.trim.length <= 39))
      .verifying("ssttp.direct-debit.form.error.accountName.check", _.matches("^[a-zA-Z '.&/]{1,39}$")), // regex from API#1856
    "sortCode" -> text
      .verifying("ssttp.direct-debit.form.error.sortCode.required", _.trim != "")
      .verifying("ssttp.direct-debit.form.error.sortCode.not-valid", x => condTrue(x.trim != "", isValidSortCode(x))),
    "accountNumber" -> text.verifying("ssttp.direct-debit.form.error.accountNumber.required", _.trim != "")
      .verifying("ssttp.direct-debit.form.error.accountNumber.not-valid", x => (x.trim == "") | (isValidNumber(x, x.length) && validateNumberLength(x)))
  )({ case (name, sc, acctNo) => ArrangementDirectDebit(name, cleanSortCode(sc), acctNo) }
    )({ case arrangementDirectDebit => Some((arrangementDirectDebit.accountName, arrangementDirectDebit.sortCode, arrangementDirectDebit.accountNumber)) })

  def isValidSortCode(sortCode: String): Boolean = {
    hasValidSortCodeCharacters(sortCode) && hasSixDigits(sortCode)
  }

  private def hasSixDigits(sortCode: String): Boolean =
    countNumeralsIn(sortCode) == 6

  private def hasValidSortCodeCharacters(sortCode: String): Boolean = {
    sortCode
      .trim
      .replaceAll("\\d", "")
      .replaceAll("-", "")
      .replaceAll(" ", "")
      .isEmpty
  }

  private def countNumeralsIn(number: String): Int =
    number.replaceAll("[^0-9]", "").length

  private def validateNumberLength(number: String): Boolean = {
    val length = countNumeralsIn(number)
    length >= 6 && length <= 8
  }

  private def isValidNumber(number: String, length: Int): Boolean = {
    countNumeralsIn(number) == length
  }

  val directDebitForm = Form(directDebitMapping)
  val directDebitFormWithBankAccountError = directDebitForm.copy(errors = Seq(FormError(" ", "ssttp.direct-debit.form.bank-not-found-info")))

}
