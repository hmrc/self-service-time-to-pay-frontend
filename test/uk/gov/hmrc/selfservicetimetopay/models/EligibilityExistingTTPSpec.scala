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

package uk.gov.hmrc.selfservicetimetopay.models

import uk.gov.hmrc.play.test.UnitSpec

class EligibilityExistingTTPSpec extends UnitSpec {

  "EligibilityExistingTTP" should {

    "create with true option parameter" in {
      val ettp = EligibilityExistingTTP(Option(true))
      ettp.hasExistingTTP.get should be(true)
    }

    "create with false option parameter" in {
      val ettp = EligibilityExistingTTP(Option(false))
      ettp.hasExistingTTP.get should be(false)
    }

    "create with false no option parameter" in {
      val ettp = EligibilityExistingTTP(None)
      ettp.hasExistingTTP.isEmpty should be(true)
    }
  }
}
