/*
 * Copyright 2018 HM Revenue & Customs
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

class EligibilityFormSpec extends PlaySpec {

  "EligibilityForm.typeOfTaxForm" must {

    "return no errors with valid data " in {
      val postData = Json.obj("type_of_tax.hasSelfAssessmentDebt" -> "true", "type_of_tax.hasOtherDebt" -> "false")

      val validatedForm = EligibilityForm.typeOfTaxForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return an error if none of the two options hasSelfAssessmentDebt and hasOtherDebt are selected" in {
      val validatedForm = EligibilityForm.typeOfTaxForm.bind(Json.obj("type_of_tax" -> ""))

      assert(validatedForm.errors.contains(FormError("type_of_tax", List("ssttp.eligibility.form.type_of_tax.required"))))
    }
  }

  "EligibilityForm.existingTtpForm" must {

    "return no errors with valid data" in {
      val postData = Json.obj("hasExistingTTP" -> "false")

      val validatedForm = EligibilityForm.existingTtpForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return an error when hasExistingTTP has not been set" in {
      val validatedForm = EligibilityForm.existingTtpForm.bind(Json.obj("hasExistingTTP" -> ""))

      assert(validatedForm.errors.contains(FormError("hasExistingTTP", List("ssttp.eligibility.form.existing_ttp.required"))))
    }
  }

  "EligibilityForm.signInQuestionForm" must {

    "return no errors with valid data when enter in manually is selected" in {
      val postData = Json.obj("signIn" -> "false")

      val validatedForm = EligibilityForm.signInQuestionForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return no errors with valid data when sign in is selected" in {
      val postData = Json.obj("signIn" -> "true")

      val validatedForm = EligibilityForm.signInQuestionForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return an error when the user does not pick an answer" in {
      val validatedForm = EligibilityForm.signInQuestionForm.bind(Json.obj())

      assert(validatedForm.errors.contains(FormError("signIn", List("ssttp.eligibility.form.sign_in_question.required"))))
    }
  }
}
