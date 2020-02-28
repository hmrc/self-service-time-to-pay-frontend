/*
 * Copyright 2020 HM Revenue & Customs
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

package eligibility

import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.selfservicetimetopay.models.{DebtIsInsignificant, DebtTooOld, EligibilityStatus, IsNotOnIa, NoDebt, NoDueDate, OldDebtIsTooHigh, Reason, ReturnNeedsSubmitting, TTPIsLessThenTwoMonths, TotalDebtIsTooHigh}

class ArrangementControllerSpec extends WordSpecLike with GuiceOneAppPerSuite with Matchers {

  def testMethod(eligibilityStatus: EligibilityStatus): String = {
    if (eligibilityStatus.eligible) "Is Eligible"
    else if (eligibilityStatus.reasons.contains(DebtTooOld) ||
      eligibilityStatus.reasons.contains(OldDebtIsTooHigh) ||
      eligibilityStatus.reasons.contains(NoDueDate) ||
      eligibilityStatus.reasons.contains(NoDebt) ||
      eligibilityStatus.reasons.contains(TTPIsLessThenTwoMonths) ||
      eligibilityStatus.reasons.contains(NoDueDate)) "Not Eligible"
    else if (eligibilityStatus.reasons.contains(IsNotOnIa)) "NotOnIa"
    else if (eligibilityStatus.reasons.contains(TotalDebtIsTooHigh)) "Over Ten Thousand"
    else if (eligibilityStatus.reasons.contains(ReturnNeedsSubmitting) || eligibilityStatus.reasons.contains(DebtIsInsignificant)) "You Need To File"
    else "EXCEPTION"
  }

  "your mum" should {
        "return Not Eligible when given a NoDebt ineligibility status" in {
          testMethod(EligibilityStatus(false, Seq(NoDebt))) shouldBe "Not Eligible"
        }

    "return Not Eligible when given a NoDebt ineligibility status1" in {
      testMethod(EligibilityStatus(false, List(NoDebt, ReturnNeedsSubmitting))) shouldBe "Not Eligible"
    }

    "return Not Eligible when given a NoDebt ineligibility status2" in {
      testMethod(EligibilityStatus(false, List(ReturnNeedsSubmitting, NoDebt))) shouldBe "Not Eligible"
    }

    "return Not Eligible when given a NoDebt ineligibility status3" in {
      testMethod(EligibilityStatus(false, Seq(ReturnNeedsSubmitting, NoDebt))) shouldBe "Not Eligible"
    }

    "return Not Eligible when given a NoDebt ineligibility status4" in {
      testMethod(EligibilityStatus(false, Seq(NoDebt, ReturnNeedsSubmitting))) shouldBe "Not Eligible"
    }

    "return not on ia page when user is not on ia" in {
      testMethod(EligibilityStatus(false, Seq(IsNotOnIa))) shouldBe "NotOnIa"
    }
  }


}
