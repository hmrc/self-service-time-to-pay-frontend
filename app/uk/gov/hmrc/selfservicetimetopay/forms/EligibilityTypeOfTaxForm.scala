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
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.selfservicetimetopay.models.EligibilityTypeOfTax

object EligibilityTypeOfTaxForm {

  def atLeastOneRequired: Constraint[(Boolean, Boolean)] = Constraint[(Boolean, Boolean)]("constraint.required") { data  =>
    if (!data._1 && !data._2) Invalid(ValidationError("ssttp.eligibility.form.type_of_tax.required")) else Valid
  }

  val typeOfTaxTuple = tuple(
    "hasSelfAssessmentDebt" -> boolean,
    "hasOtherDebt" -> boolean
  )

  val typeOfTaxForm = Form(mapping(
    "type_of_tax" -> typeOfTaxTuple.verifying(atLeastOneRequired)
  )(type_of_tax => EligibilityTypeOfTax(type_of_tax._1, type_of_tax._2))
  (type_of_tax => Some((type_of_tax.hasSelfAssessmentDebt, type_of_tax.hasOtherDebt))))
}
