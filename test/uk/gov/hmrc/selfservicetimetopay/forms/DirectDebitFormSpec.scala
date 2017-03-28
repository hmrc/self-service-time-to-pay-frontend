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

import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.libs.json.Json

class DirectDebitFormSpec extends PlaySpec {

  "DirectDebitForm.directDebitForm" must {

    "return no errors with valid data" in {
      val postData = Json.obj(
        "accountName" -> "Bob",
        "sortCode1" -> "12",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return error with invalid special characters in accountName" in {
      val postData = Json.obj(
        "accountName" -> "John Smith####[]{}",
        "sortCode1" -> "12",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("accountName", List("ssttp.direct-debit.form.error.accountName.not-text"))))
    }

    "return error with sortCode containing too few numbers" in {
      val postData = Json.obj(
        "accountName" -> "Bob",
        "sortCode1" -> "1",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("sortCode1", List("ssttp.direct-debit.form.error.sortCode.not-valid"))))
    }

    "return error with accountNumber containing too few numbers" in {
      val postData = Json.obj(
        "accountName" -> "Bob",
        "sortCode1" -> "1",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "12345"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("accountNumber", List("ssttp.direct-debit.form.error.accountNumber.not-valid"))))
    }

    "return error with sortCode containing an invalid character" in {
      val postData = Json.obj(
        "accountName" -> "Bob",
        "sortCode1" -> "12",
        "sortCode2" -> "34",
        "sortCode3" -> "5e",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("sortCode3", List("ssttp.direct-debit.form.error.sortCode.not-valid"))))
    }

    "return error with accountNumber containing an invalid character" in {
      val postData = Json.obj(
        "accountName" -> "Bob",
        "sortCode1" -> "12",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "1234567e"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("accountNumber", List("ssttp.direct-debit.form.error.accountNumber.not-valid"))))
    }

    "return error with empty accountName" in {
      val postData = Json.obj(
        "accountName" -> "",
        "sortCode1" -> "12",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("accountName", List("ssttp.direct-debit.form.error.accountName.required"))))
    }

    "return error with accountName not starting with a letter" in {
      val postData = Json.obj(
        "accountName" -> "123456789",
        "sortCode1" -> "12",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("accountName", List("ssttp.direct-debit.form.error.accountName.letter-start"))))
    }

    "return error with accountName containing numbers" in {
      val postData = Json.obj(
        "accountName" -> "a12345678",
        "sortCode1" -> "12",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("accountName", List("ssttp.direct-debit.form.error.accountName.not-text"))))
    }

    "return no error when accountName contains valid special characters" in {
      val postData = Json.obj(
        "accountName" -> "John Smith''''''&&&",
        "sortCode1" -> "12",
        "sortCode2" -> "34",
        "sortCode3" -> "56",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return error with decimal place numbers in sortCode" in {
      val postData = Json.obj(
        "accountName" -> "John Smith'",
        "sortCode1" -> "12",
        "sortCode2" -> "3.",
        "sortCode3" -> "56",
        "accountNumber" -> "12345678"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("sortCode2", List("ssttp.direct-debit.form.error.sortCode.not-valid"))))
    }

    "return error with decimal place numbers in accountNumber" in {
      val postData = Json.obj(
        "accountName" -> "John Smith'",
        "sortCode1" -> "12",
        "sortCode2" -> "32",
        "sortCode3" -> "56",
        "accountNumber" -> "123456.7"
      )

      val validatedForm = DirectDebitForm.directDebitForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("accountNumber", List("ssttp.direct-debit.form.error.accountNumber.not-valid"))))
    }
  }
}
