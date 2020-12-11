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

package ssttpcalculator

import java.io
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.Month.{APRIL, JANUARY, NOVEMBER}
import java.time.chrono.ChronoLocalDate
import java.time.format.DateTimeFormatter

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import ssttpcalculator.CalculatorService._
import testsupport.UnitSpec
import timetopaytaxpayer.cor.model._
import uk.gov.hmrc.http.HeaderCarrier
import testsupport.testdata.TdAll.{communicationPreferences, saUtr}

class CalculatorServiceMaximumDurationInMonthsSpec extends UnitSpec with TableDrivenPropertyChecks {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  type TableT = TableFor3[LocalDate, SelfAssessmentDetails, Int]

  private def debit(
      originCode: String,
      taxYear:    String,
      value:      BigDecimal,
      dueDate:    LocalDate,
      interest:   Option[BigDecimal] = None
  ) =
    Debit(
      originCode = originCode,
      amount     = value,
      dueDate    = dueDate,
      interest   = Some(Interest(Some(now()), interest.getOrElse(BigDecimal(0)))),
      taxYearEnd = LocalDate.parse(taxYear, formatter)
    )

  private val return2016 =
    Return(
      taxYearEnd   = "2016-04-05",
      issuedDate   = None,
      dueDate      = "2017-01-31",
      receivedDate = None
    )
  private val return2017 =
    Return(
      taxYearEnd   = "2017-04-05",
      issuedDate   = None,
      dueDate      = "2018-01-31",
      receivedDate = None
    )
  private val return2018 = Return(
    taxYearEnd   = "2018-04-05",
    issuedDate   = None,
    dueDate      = None,
    receivedDate = None
  )

  private def selfAssessmentDetails(
      debits:  Seq[Debit]  = Seq.empty,
      returns: Seq[Return] = List(return2016)
  ) =
    SelfAssessmentDetails(
      utr                      = saUtr,
      communicationPreferences = communicationPreferences,
      debits                   = debits,
      returns                  = returns
    )

  //Note the calculator logic is not responsible for determining eligibility
  private val scenariosBasedAroundJanuaryDueDate: TableT = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 1
    (
      "2017-01-02",
      selfAssessmentDetails(debits = List(
        debit("BCD", "2016-04-05", 1000, "2017-01-31"),
        debit("POA1", "2016-04-05", 2000, "2017-01-31"),
        debit("POA2", "2016-04-05", 2000, "2017-01-31"))
      ),
      11
    ),

    //Customer 2
    (
      "2017-02-14",
      selfAssessmentDetails(debits = List(
        debit("BCD", "2016-04-05", 1000, "2017-01-31"),
        debit("POA1", "2016-04-05", 3000, "2017-01-31", Some(40)),
        debit("POA2", "2016-04-05", 3000, "2017-07-31"))
      ),
      10
    ),

    // Customer 4
    (
      "2017-01-10",
      selfAssessmentDetails(debits = List(
        debit("BCD", "2016-04-05", 1000, "2017-01-31"),
        debit("POA1", "2016-04-05", 3000, "2017-01-31", Some(40)),
        debit("POA2", "2016-04-05", 3000, "2017-07-31"),
        debit("Unpaid interest from previous arrangement", "2016-04-05", 28, "2016-01-31"))
      ),
      11
    )
  )

  private val scenariosBasedAroundJulyDuedate: TableT = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 9
    ("2017-07-01",
      selfAssessmentDetails(debits = List(
        debit("POA2", "2016-04-05", 5000, "2017-07-31"))
      ),
      5
    ),
    //Customer 10
    (
      "2017-08-14",
      selfAssessmentDetails(debits = List(
        debit("POA2", "2016-04-05", 3000, "2017-07-31", Some(20)))
      ),
      4
    ),
    //Customer 11
    (
      "2017-07-01",
      selfAssessmentDetails(debits = List(
        debit("POA2", "2016-04-05", 3000, "2017-07-31"),
        debit("Interest from previous arrangement", "2015-04-05", 28, "2016-01-31"))
      ),
      5
    ),
    //Customer 13
    (
      "2017-07-01",
      selfAssessmentDetails(debits = List(
        debit("POA2", "2016-04-05", 3000, "2017-07-31"))
      ),
      5
    )
  )

  private val scenariosBasedAroundEarlyFilers: TableT = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 15
    ("2017-01-02".withMonth(7).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(debit("POA2", "2016-04-05", 5000, "2017-07-31")),
      returns = List(return2016, return2017)), 11),
    //Customer 16
    ("2017-01-02".withMonth(7).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(
        debit("POA2", "2016-04-05", 5000, "2017-07-31"),
        debit("BCD", "2017-04-05", 2000, "2018-01-18")),
      returns = List(return2016, return2017)), 11),
    //Customer 17
    ("2017-01-02".withMonth(7).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(
        debit("POA2", "2016-04-05", 5000, "2017-07-31"),
        debit("BCD", "2017-04-05", 2000, "2018-01-31"),
        debit("POA1", "2017-04-05", 1000, "2018-01-31"),
        debit("POA2", "2017-04-05", 1000, "2018-01-31")),
      returns = List(return2016, return2017)), 11),
    //Customer 19
    ("2017-01-02".withMonth(5).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(
        debit("BCD", "2017-04-05", 1000, "2018-01-31"),
        debit("POA1", "2017-04-05", 2000, "2018-01-31"),
        debit("POA2", "2017-04-05", 3000, "2018-01-31")),
      returns = List(return2016, return2017)), 11)
  )

  private val scenariosBasedAroundLateFilers = Table[LocalDate, SelfAssessmentDetails, Int](
    ("todayDate", "self assessment", "expected answer"),
    //Customer 20
    //todo I am assuming the old charges will not appear in des?
    ("2017-01-02".withMonth(7).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(
        debit("BCD", "2017-04-05", 0, "2017-07-31"),
        debit("Late payment penalty", "2015-04-05", 1000, "2017-04-28")),
      returns = List(return2016, return2017)), 9),
    //Customer 21
    //todo will the Late payment penalty appear as two paymeant
    ("2017-01-02".withMonth(4).withDayOfMonth(20), selfAssessmentDetails(
      debits  = List(
        debit("BCD", "2017-04-05", 0, "2017-07-31"),
        debit("Late payment penalty", "2016-04-05", 1000, "2017-04-28"),
        debit("POA2", "2016-04-05", 5000, "2017-07-17")),
      returns = List(return2016, return2017)), 11),
    //Customer 22
    ("2017-01-02".withMonth(3).withDayOfMonth(20), selfAssessmentDetails(
      debits  = List(
        debit("Late payment penalty", "2016-04-05", 100, "2017-03-25")),
      returns = List(return2016, return2017)), 11),
    //Customer 24
    ("2017-01-02".withMonth(3).withDayOfMonth(20), selfAssessmentDetails(
      debits  = List(
        debit("POA2", "2016-04-05", 3000, "2017-07-31"),
        debit("Late payment penalty", "2016-04-05", 100, "2017-03-17")),
      returns = List(return2016, return2017)), 11),
    //Customer 25
    (
      "2017-11-20",
      selfAssessmentDetails(
        debits  = List(
          debit("Late payment penalty", "2016-04-05", 100, "2016-12-16"),
          debit("BCD", "2016-04-05", 50, "2017-01-31"),
          debit("POA1", "2016-04-05", 1100, "2017-01-31"),
          debit("POA2", "2016-04-05", 1100, "2017-01-31")),
        returns = List(return2016, return2017)), 0)
  )

  private val scenariosBasedCustom = Table(
    ("todayDate", "self assessment", "expected answer"),
    ("2017-01-02".withMonth(12).withDayOfMonth(18), selfAssessmentDetails(
      debits  = List(
        debit("IN1", "2018-04-05", 30, "2018-01-31"),
        debit("IN2", "2018-04-05", 500, "2018-07-31")),
      returns = List(return2017.copy(receivedDate = Some(LocalDate.of(2017, NOVEMBER, 24))), return2018)), 11))

  //todo ask ela about 11 13 15
  "maximumDurationInMonths for" - {
    TableDrivenPropertyChecks.forAll(scenariosBasedAroundJanuaryDueDate) { (startDate: LocalDate, selfA: SelfAssessmentDetails, answer: Int) =>
      s"scenarios Based Around January Due date return the correct number of months($answer) for start date : $startDate and SelfAssessmentDetails $selfA" in {
        val result: Int = maximumDurationInMonths(selfA, startDate)
        assert(result == answer)
      }
    }

    TableDrivenPropertyChecks.forAll(scenariosBasedAroundJulyDuedate) { (startDate: LocalDate, selfA: SelfAssessmentDetails, answer: Int) =>
      s"scenarios Based Around July Due date return the correct number of months($answer) for start date : $startDate and SelfAssessmentDetails $selfA" in {
        val result: Int = maximumDurationInMonths(selfA, startDate)
        assert(result == answer)

      }
    }

    TableDrivenPropertyChecks.forAll(scenariosBasedAroundEarlyFilers) { (startDate: LocalDate, selfA: SelfAssessmentDetails, answer: Int) =>
      s"scenarios Based Around Early Filers Due date return the correct number of months($answer) for start date : $startDate and SelfAssessmentDetails $selfA" in {
        val result: Int = maximumDurationInMonths(selfA, startDate)
        assert(result == answer)
      }
    }

    TableDrivenPropertyChecks.forAll(scenariosBasedAroundLateFilers) { (today: LocalDate, sa: SelfAssessmentDetails, answer: Int) =>
      s"scenarios Based Around Late Filers Due date return the correct number of months($answer) for start date : $today and SelfAssessmentDetails $sa" in {
        val result: Int = maximumDurationInMonths(sa, today)

        val maxDurationBasedOnDebits = maximumDurationInMonthsBasedOnDebits(sa, today)
        val maxDurationBasedOnReturns = maximumDurationInMonthsBasedOnDebits(sa, today)

        result shouldBe answer withClue
          s"""
             |today: $today
             |maxDurationBasedOnDebits: $maxDurationBasedOnDebits
             |maxDurationBasedOnReturns:  $maxDurationBasedOnReturns
             |sa:
             |${Json.prettyPrint(Json.toJson(sa))}
             |""".stripMargin
      }
    }

    TableDrivenPropertyChecks.forAll(scenariosBasedCustom) { (today: LocalDate, selfA: SelfAssessmentDetails, answer: Int) =>
      s"scenarios Based Around Custom the correct number of months($answer) for start date : $today and SelfAssessmentDetails $selfA" in {
        val result: Int = maximumDurationInMonths(selfA, today)
        result shouldBe answer
      }
    }
  }

  implicit def toLocalDate(s: String): LocalDate = LocalDate.parse(s)

  implicit def toOption[A](a: A): Option[A] = Some(a)

  implicit def toOptionDate(s: String): Option[LocalDate] = Some(s)
}
