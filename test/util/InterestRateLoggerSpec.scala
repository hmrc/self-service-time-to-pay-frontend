
package util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ssttpcalculator.model.InterestRate

import java.time.LocalDate

class InterestRateLoggerSpec extends AnyWordSpec with Matchers {
  val logger = new JourneyLogger(getClass)

  "Logs applicable interest rates passed to in easy-to-read format" when {
    "only one applicable interest rate is passed (in the sequence)" should {
      "log a single message with an interest applicable from its start date 'onwards'" in {
        val rates = Seq(InterestRate(LocalDate.parse("2020-02-05"), LocalDate.parse("2020-07-31"), 2.50))

        logger.applicableInterestRateLogMessages(rates) shouldBe Seq(
          "[from 2020-02-05 onwards => 2.5%]"
        )
      }
    }

    "multiple applicable interest rates are passed (in the sequence)" should {
      "each rate is logged separately, starting with the the oldest end date, showing bounded periods except the last" in {
        val rates = Seq(
          InterestRate(LocalDate.parse("2020-02-05"), LocalDate.parse("2020-07-31"), 2.50),
          InterestRate(LocalDate.parse("2020-08-01"), LocalDate.parse("2020-08-31"), 2.75),
          InterestRate(LocalDate.parse("2020-09-01"), LocalDate.parse("2020-10-31"), 3.00)
        )

        logger.applicableInterestRateLogMessages(rates) shouldBe Seq(
          "[from 2020-02-05 to 2020-07-31 => 2.5%]",
          "[from 2020-08-01 to 2020-08-31 => 2.75%]",
          "[from 2020-09-01 onwards => 3.0%]"
        )
      }
      "order each period from oldest to most recent even if the rates passed are not in order" in {
        val rates = Seq(
          InterestRate(LocalDate.parse("2020-08-01"), LocalDate.parse("2020-08-31"), 2.75),
          InterestRate(LocalDate.parse("2020-02-05"), LocalDate.parse("2020-07-31"), 2.50),
          InterestRate(LocalDate.parse("2020-09-01"), LocalDate.parse("2020-10-31"), 3.00)
        )

        logger.applicableInterestRateLogMessages(rates) shouldBe Seq(
          "[from 2020-02-05 to 2020-07-31 => 2.5%]",
          "[from 2020-08-01 to 2020-08-31 => 2.75%]",
          "[from 2020-09-01 onwards => 3.0%]"
        )
      }
    }
    "no rates are given" should {
      "log no messages" in {
        val rates = Seq()

        logger.applicableInterestRateLogMessages(rates) shouldBe Seq()
      }
    }

  }

}
