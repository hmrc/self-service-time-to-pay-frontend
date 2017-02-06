package uk.gov.hmrc.selfservicetimetopay.models

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
class ArrangementDirectDebitTest extends UnitSpec  with MockitoSugar {

  val sampleArrangementDirectDebit = ArrangementDirectDebit("Mr.Colm CAvnaagh", "123456", "12345678")

  "ArrangementDirectDebit" should {
    "format the sort code correctly " in {
      {
        sampleArrangementDirectDebit.formatSortCode shouldEqual "12 - 34 - 56"
      }

    }
  }
}
