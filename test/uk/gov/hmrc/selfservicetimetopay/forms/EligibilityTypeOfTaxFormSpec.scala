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

import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.libs.json.Json

class EligibilityTypeOfTaxFormSpec extends PlaySpec {

  "EligibilityTypeOfTaxForm" must {

    "return no errors with valid data" in {
      val postData = Json.obj("type_of_tax" -> Json.obj("hasSelfAssessmentDebt" -> true, "hasOtherDebt" -> false))

      val validatedForm = EligibilityTypeOfTaxForm.typeOfTaxForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return an error if none of the two options hasSelfAssessmentDebt and hasOtherDebt are selected" in {
      val postData = Json.obj("type_of_tax" -> Json.obj("hasSelfAssessmentDebt" -> "", "hasOtherDebt" -> ""))

      val validatedForm = EligibilityTypeOfTaxForm.typeOfTaxForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("hasSelfAssessmentDebt", List("ssttp.eligibility.form.type_of_tax.required"))))
    }
  }
}
