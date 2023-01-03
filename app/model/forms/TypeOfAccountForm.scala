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

package model.forms

import enumformat.EnumFormatter
import model.enumsforforms.{IsSoleSignatory, TypeOfBankAccount, TypesOfBankAccount}
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms, Mapping}
import play.api.i18n.Messages

final case class TypeOfAccountForm(typeOfAccount: TypeOfBankAccount, isSoleSignatory: IsSoleSignatory)

object TypeOfAccountForm {

  def form(implicit messages: Messages): Form[TypeOfAccountForm] = {
    val typeOfAccountFormMapping: Mapping[TypeOfBankAccount] = Forms.of(EnumFormatter.format(
      enum                    = TypesOfBankAccount,
      errorMessageIfMissing   = Messages("ssttp.arrangement.type_of_account.error"),
      errorMessageIfEnumError = Messages("ssttp.arrangement.type_of_account.error")
    ))
    Form(
      mapping(
        "typeOfAccount" -> typeOfAccountFormMapping,
        "isSoleSignatory" -> isSoleSignatoryFormMapping
      )(TypeOfAccountForm.apply)(TypeOfAccountForm.unapply)
    )
  }

  private def isSoleSignatoryFormMapping(implicit messages: Messages): Mapping[IsSoleSignatory] = Forms.of(EnumFormatter.format(
    enum                    = IsSoleSignatory,
    errorMessageIfMissing   = Messages("ssttp.arrangement.sole_signatory.error"),
    errorMessageIfEnumError = Messages("ssttp.arrangement.sole_signatory.error")
  ))
}
