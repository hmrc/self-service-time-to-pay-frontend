package model.forms

import model.{TypeOfBankAccount, TypesOfBankAccount}
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms, Mapping}
import utils.EnumFormatter

final case class TypeOfAccountForm(typeOfAccount: TypeOfBankAccount)

object TypeOfAccountForm {
  def form: Form[TypeOfAccountForm] = {
    val typeOfAccountFormMapping: Mapping[TypeOfBankAccount] = Forms.of(EnumFormatter.format(
      enum                    = TypesOfBankAccount,
      errorMessageIfMissing   = "enter-type-of-account.missing",
      errorMessageIfEnumError = "enter-type-of-account.missing"
    ))
    Form(
      mapping(
        "typeOfAccount" -> typeOfAccountFormMapping
      )(TypeOfAccountForm.apply)(TypeOfAccountForm.unapply)
    )
  }
}
