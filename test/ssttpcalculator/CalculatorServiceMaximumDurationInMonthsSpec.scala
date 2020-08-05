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

import java.time.LocalDate
import java.time.LocalDate.now
import java.time.Month.{APRIL, JANUARY, NOVEMBER}
import java.time.format.DateTimeFormatter

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import ssttpcalculator.CalculatorService._
import timetopaytaxpayer.cor.model._
import _root_.uk.gov.hmrc.http.HeaderCarrier
import testsupport.testdata.TdAll.{communicationPreferences, saUtr}

class CalculatorServiceMaximumDurationInMonthsSpec extends PlaySpec with TableDrivenPropertyChecks {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private def debit(des: String, taxYear: String, value: BigDecimal, dueDate: String, interest: Option[BigDecimal] = None) =
    Debit(
      originCode = des,
      amount     = value,
      dueDate    = LocalDate.parse(dueDate, formatter),
      interest   = Some(Interest(Some(now()), interest.getOrElse(BigDecimal(0)))),
      taxYearEnd = LocalDate.parse(taxYear, formatter))

  private val startDate = LocalDate.of(2017, JANUARY, 2)

  private val return2016 =
    Return(LocalDate.of(2016, APRIL, 5), None, Some(LocalDate.of(2017, JANUARY, 31)), None)
  private val return2017 =
    Return(LocalDate.of(2017, APRIL, 5), None, Some(LocalDate.of(2018, JANUARY, 31)), None)
  private val return2018 = Return(LocalDate.of(2018, APRIL, 5), None, None, None)

  private def selfAssessmentDetails(debits: Seq[Debit] = Seq.empty, returns: Seq[Return] = List(return2016)) =
    SelfAssessmentDetails(saUtr, CommunicationPreferences(false, false, false, false), debits, returns)

  //Note the calculator logic is not responsible for determining eligibility
  private val scenariosBasedAroundJanuaryDuedate = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 1
    (startDate, selfAssessmentDetails(debits = List(
      debit("BCD", "2016-04-05", 1000, "2017-01-31"),
      debit("POA1", "2016-04-05", 2000, "2017-01-31"),
      debit("POA2", "2016-04-05", 2000, "2017-01-31"))), 11),

    //Customer 2
    (startDate.withMonth(2).withDayOfMonth(14), selfAssessmentDetails(debits = List(
      debit("BCD", "2016-04-05", 1000, "2017-01-31"),
      debit("POA1", "2016-04-05", 3000, "2017-01-31", Some(40)),
      debit("POA2", "2016-04-05", 3000, "2017-07-31"))), 10),

    // Customer 4
    (startDate.withMonth(1).withDayOfMonth(10), selfAssessmentDetails(debits = List(
      debit("BCD", "2016-04-05", 1000, "2017-01-31"),
      debit("POA1", "2016-04-05", 3000, "2017-01-31", Some(40)),
      debit("POA2", "2016-04-05", 3000, "2017-07-31"),
      debit("Unpaid interest from previous arrangement", "2016-04-05", 28, "2016-01-31"))), 11)
  )

  private val scenariosBasedAroundJulyDuedate = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 9
    (startDate.withMonth(7).withDayOfMonth(1), selfAssessmentDetails(debits = List(
      debit("POA2", "2016-04-05", 5000, "2017-07-31"))), 5),
    //Customer 10
    (startDate.withMonth(8).withDayOfMonth(14), selfAssessmentDetails(debits = List(
      debit("POA2", "2016-04-05", 3000, "2017-07-31", Some(20)))), 4),
    //Customer 11
    (startDate.withMonth(7).withDayOfMonth(1), selfAssessmentDetails(debits = List(
      debit("POA2", "2016-04-05", 3000, "2017-07-31"),
      debit("Interest from previous arrangement", "2015-04-05", 28, "2016-01-31"))), 5),
    //Customer 13
    (startDate.withMonth(7).withDayOfMonth(1), selfAssessmentDetails(debits = List(
      debit("POA2", "2016-04-05", 3000, "2017-07-31"))), 5))

  private val scenariosBasedAroundEarlyFilers = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 15
    (startDate.withMonth(7).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(debit("POA2", "2016-04-05", 5000, "2017-07-31")),
      returns = List(return2016, return2017)), 11),
    //Customer 16
    (startDate.withMonth(7).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(
        debit("POA2", "2016-04-05", 5000, "2017-07-31"),
        debit("BCD", "2017-04-05", 2000, "2018-01-18")),
      returns = List(return2016, return2017)), 11),
    //Customer 17
    (startDate.withMonth(7).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(
        debit("POA2", "2016-04-05", 5000, "2017-07-31"),
        debit("BCD", "2017-04-05", 2000, "2018-01-31"),
        debit("POA1", "2017-04-05", 1000, "2018-01-31"),
        debit("POA2", "2017-04-05", 1000, "2018-01-31")),
      returns = List(return2016, return2017)), 11),
    //Customer 19
    (startDate.withMonth(5).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(
        debit("BCD", "2017-04-05", 1000, "2018-01-31"),
        debit("POA1", "2017-04-05", 2000, "2018-01-31"),
        debit("POA2", "2017-04-05", 3000, "2018-01-31")),
      returns = List(return2016, return2017)), 11)
  )

  private val scenariosBasedAroundLateFilers = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 20
    //todo I am assuming the old charges will not appear in des?
    (startDate.withMonth(7).withDayOfMonth(1), selfAssessmentDetails(
      debits  = List(
        debit("BCD", "2017-04-05", 0, "2017-07-31"),
        debit("Late payment penalty", "2015-04-05", 1000, "2017-04-28")),
      returns = List(return2016, return2017)), 11),
    //Customer 21
    //todo will the Late payment penalty appear as two paymeant
    (startDate.withMonth(4).withDayOfMonth(20), selfAssessmentDetails(
      debits  = List(
        debit("BCD", "2017-04-05", 0, "2017-07-31"),
        debit("Late payment penalty", "2016-04-05", 1000, "2017-04-28"),
        debit("POA2", "2016-04-05", 5000, "2017-07-17")),
      returns = List(return2016, return2017)), 11),
    //Customer 22
    (startDate.withMonth(3).withDayOfMonth(20), selfAssessmentDetails(
      debits  = List(
        debit("Late payment penalty", "2016-04-05", 100, "2017-03-25")),
      returns = List(return2016, return2017)), 11),
    //Customer 24
    (startDate.withMonth(3).withDayOfMonth(20), selfAssessmentDetails(
      debits  = List(
        debit("POA2", "2016-04-05", 3000, "2017-07-31"),
        debit("Late payment penalty", "2016-04-05", 100, "2017-03-17")),
      returns = List(return2016, return2017)), 11),
    //Customer 25
    (startDate.withMonth(11).withDayOfMonth(20), selfAssessmentDetails(
      debits  = List(
        debit("Late payment penalty", "2016-04-05", 100, "2016-12-16"),
        debit("BCD", "2016-04-05", 50, "2017-01-31"),
        debit("POA1", "2016-04-05", 1100, "2017-01-31"),
        debit("POA2", "2016-04-05", 1100, "2017-01-31")),
      returns = List(return2016, return2017)), 11)
  )

  private val scenariosBasedCustom = Table(
    ("todayDate", "self assessment", "expected answer"),
    (startDate.withMonth(12).withDayOfMonth(18), selfAssessmentDetails(
      debits  = List(
        debit("IN1", "2018-04-05", 30, "2018-01-31"),
        debit("IN2", "2018-04-05", 500, "2018-07-31")),
      returns = List(return2017.copy(receivedDate = Some(LocalDate.of(2017, NOVEMBER, 24))), return2018)), 11))

  //todo ask ela about 11 13 15
  "maximumDurationInMonths for" should {
    TableDrivenPropertyChecks.forAll(scenariosBasedAroundJanuaryDuedate) { (startDate: LocalDate, selfA: SelfAssessmentDetails, answer: Int) =>
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

    TableDrivenPropertyChecks.forAll(scenariosBasedAroundEarlyFilers) { (startDate: LocalDate, selfA: SelfAssessmentDetails, answer: Int) =>
      s"scenarios Based Around Late Filers Due date return the correct number of months($answer) for start date : $startDate and SelfAssessmentDetails $selfA" in {
        val result: Int = maximumDurationInMonths(selfA, startDate)
        assert(result == answer)
      }
    }

    TableDrivenPropertyChecks.forAll(scenariosBasedCustom) { (startDate: LocalDate, selfA: SelfAssessmentDetails, answer: Int) =>
      s"scenarios Based Around Custom the correct number of months($answer) for start date : $startDate and SelfAssessmentDetails $selfA" in {
        val result: Int = maximumDurationInMonths(selfA, startDate)
        assert(result == answer)
      }
    }
  }
}
