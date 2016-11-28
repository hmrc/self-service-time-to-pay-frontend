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

package uk.gov.hmrc.selfservicetimetopay.controllers

import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.models.EligibilityTypeOfTax
import uk.gov.hmrc.selfservicetimetopay._

class EligibilityControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  private val gaToken = "GA-TOKEN"
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map("google-analytics.token" -> gaToken))

  val typeOfTaxForm = EligibilityTypeOfTax(
    hasSelfAssessmentDebt = true
  )

  "EligibilityController" should {
    val controller = new EligibilityController()

    "redirect successfully to the type of tax page" in {
      val response:Result = controller.present.apply(FakeRequest())

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.EligibilityController.typeOfTaxPresent().url
    }

    "successfully display the type of tax page" in {
      val response:Result = controller.typeOfTaxPresent.apply(FakeRequest())

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.eligibility.form.type_of_tax.title"))
    }

    "successfully display the existing ttp page" in {
      val response = await(controller.existingTtpPresent.apply(FakeRequest()))

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.eligibility.form.existing_ttp.title"))
    }
  }

}
