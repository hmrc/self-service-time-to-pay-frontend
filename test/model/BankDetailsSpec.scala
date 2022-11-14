
package model

import testsupport.UnitSpec
import uk.gov.hmrc.selfservicetimetopay.models.BankDetails
import uk.gov.hmrc.selfservicetimetopay.models.BankDetails.truncateAccName

class BankDetailsSpec extends UnitSpec {

  val string60Long: String = "Abc" * 20
  val string59Long: String = "Abc" * 19 + "Ab"

  case class TestCase(accountName: String)

  val testCases = List(
    TestCase(s"$string60Long"),
    TestCase(s"$string60Long"),
    TestCase(s"$string59Long")
    )

  def mkBankDetailsFromTestCases(testCase: String): BankDetails = {
    BankDetails(sortCode = "123456", accountNumber = "12345678", testCase)
  }

  "truncate" - {
    "should return an account of correct length" in {
      testCases.foreach { tc =>
        (truncateAccName(mkBankDetailsFromTestCases(tc.accountName)).accountName.length <=60) shouldBe true
      }
    }
  }

}
