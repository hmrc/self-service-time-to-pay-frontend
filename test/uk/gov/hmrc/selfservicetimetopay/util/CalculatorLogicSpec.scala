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

package uk.gov.hmrc.selfservicetimetopay.util

import java.time.{LocalDate, Month}

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.selfservicetimetopay.models.{Debit, Interest, Return, SelfAssessment}
import uk.gov.hmrc.selfservicetimetopay.service.CalculatorService._
class CalculatorLogicSpec extends PlaySpec with TableDrivenPropertyChecks {


  import java.time.format.DateTimeFormatter

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def makeDebit(des: String, taxYear: String, value: BigDecimal, dueDate: String, interest: Option[BigDecimal] = None): Debit = {
    Debit(originCode = Some(des), value, LocalDate.parse(dueDate, formatter), Some(Interest(LocalDate.now(), interest.getOrElse(0)))
      , Some(LocalDate.parse(taxYear, formatter)))
  }

  private val testStartDate = LocalDate.of(2017, Month.JANUARY, 2)
  val testReturn2016 = Return(LocalDate.of(2016, Month.APRIL, 5), None, Some(LocalDate.of(2017, Month.JANUARY, 31)), None)
  val testReturn2017 = Return(LocalDate.of(2017, Month.APRIL, 5), None, Some(LocalDate.of(2018, Month.JANUARY, 31)), None)

  val testReturn2018 = Return(LocalDate.of(2018, Month.APRIL, 5), None, None, None)
  private val testReturns = Some(List(testReturn2016))
  val sa = SelfAssessment().copy(returns = testReturns)
  //Note the calculatorLOgic is not responceable for determining eligiblity
  val scenariosBasedAroundJanuaryDuedate = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 1
    (testStartDate, sa.copy(debits
      = List(makeDebit("BCD", "2016-04-05", 1000, "2017-01-31"),
      makeDebit("POA1", "2016-04-05", 2000, "2017-01-31"),
      makeDebit("POA2", "2016-04-05", 2000, "2017-01-31"))), 11),
    //Customer 2
    (testStartDate.withMonth(2).withDayOfMonth(14), sa.copy(debits
      = List(makeDebit("BCD", "2016-04-05", 1000, "2017-01-31"),
      makeDebit("POA1", "2016-04-05", 3000, "2017-01-31", Some(40)),
      makeDebit("POA2", "2016-04-05", 3000, "2017-07-31"))), 10),
    // Customer 4
    (testStartDate.withMonth(1).withDayOfMonth(10), sa.copy(debits
      = List(makeDebit("BCD", "2016-04-05", 1000, "2017-01-31"),
      makeDebit("POA1", "2016-04-05", 3000, "2017-01-31", Some(40)),
      makeDebit("POA2", "2016-04-05", 3000, "2017-07-31"),
      makeDebit("Unpaid interest from previous arrangement", "2016-04-05", 28, "2016-01-31"))), 11)
  )
  val scenariosBasedAroundJulyDuedate = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 9
    (testStartDate.withMonth(7).withDayOfMonth(1), sa.copy(debits
      = List(makeDebit("POA2", "2016-04-05", 5000, "2017-07-31"))), 5),
    //Customer 10
    (testStartDate.withMonth(8).withDayOfMonth(14), sa.copy(debits
      = List(makeDebit("POA2", "2016-04-05", 3000, "2017-07-31", Some(20)))), 4),
    //Customer 11
    (testStartDate.withMonth(7).withDayOfMonth(1), sa.copy(debits
      = List(makeDebit("POA2", "2016-04-05", 3000, "2017-07-31"),
      makeDebit("Interest from previous arrangement", "2015-04-05", 28, "2016-01-31"))), 5),
    //Customer 13
    (testStartDate.withMonth(7).withDayOfMonth(1), sa.copy(debits
      = List(makeDebit("POA2", "2016-04-05", 3000, "2017-07-31"))), 5))

  val scenariosBasedAroundEarlyFilers = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 15
    (testStartDate.withMonth(7).withDayOfMonth(1), sa.copy(debits
      = List(makeDebit("POA2", "2016-04-05", 5000, "2017-07-31")), returns = Some(List(testReturn2016, testReturn2017))), 11),
    //Customer 16
    (testStartDate.withMonth(7).withDayOfMonth(1), sa.copy(debits
      = List(makeDebit("POA2", "2016-04-05", 5000, "2017-07-31"),
      makeDebit("BCD", "2017-04-05", 2000, "2018-01-18")), returns = Some(List(testReturn2016, testReturn2017))), 11),
    //Customer 17
    (testStartDate.withMonth(7).withDayOfMonth(1), sa.copy(debits
      = List(makeDebit("POA2", "2016-04-05", 5000, "2017-07-31"),
      makeDebit("BCD", "2017-04-05", 2000, "2018-01-31"),
      makeDebit("POA1", "2017-04-05", 1000, "2018-01-31"),
      makeDebit("POA2", "2017-04-05", 1000, "2018-01-31")), returns = Some(List(testReturn2016, testReturn2017))), 11),
    //Customer 19
    (testStartDate.withMonth(5).withDayOfMonth(1), sa.copy(debits
      = List(makeDebit("BCD", "2017-04-05", 1000, "2018-01-31"),
      makeDebit("POA1", "2017-04-05", 2000, "2018-01-31"),
      makeDebit("POA2", "2017-04-05", 3000, "2018-01-31")), returns = Some(List(testReturn2016, testReturn2017))), 11)

  )
  val scenariosBasedAroundLateFilers = Table(
    ("todayDate", "self assessment", "expected answer"),
    //Customer 20
    //todo I am assuming the old charges will not appear in des?
    (testStartDate.withMonth(7).withDayOfMonth(1), sa.copy(debits
      = List(makeDebit("BCD", "2017-04-05", 0, "2017-07-31"),
      makeDebit("Late payment penalty", "2015-04-05", 1000, "2017-04-28")), returns = Some(List(testReturn2016, testReturn2017))), 11),
    //Customer 21
    //todo will the Late payment penalty appear as two paymeant
    (testStartDate.withMonth(4).withDayOfMonth(20), sa.copy(debits
      = List(makeDebit("BCD", "2017-04-05", 0, "2017-07-31"),
      makeDebit("Late payment penalty", "2016-04-05", 1000, "2017-04-28"),
      makeDebit("POA2", "2016-04-05", 5000, "2017-07-17")), returns = Some(List(testReturn2016, testReturn2017))), 11),
    //Customer 22
    (testStartDate.withMonth(3).withDayOfMonth(20), sa.copy(debits
      = List(makeDebit("Late payment penalty", "2016-04-05", 100, "2017-03-25")), returns = Some(List(testReturn2016, testReturn2017))), 11),
    //Customer 24
    (testStartDate.withMonth(3).withDayOfMonth(20), sa.copy(debits
      = List(makeDebit("POA2", "2016-04-05", 3000, "2017-07-31"),
      makeDebit("Late payment penalty", "2016-04-05", 100, "2017-03-17")), returns = Some(List(testReturn2016, testReturn2017))), 11),
    //Customer 25
    (testStartDate.withMonth(11).withDayOfMonth(20), sa.copy(debits
      = List(makeDebit("Late payment penalty", "2016-04-05", 100, "2016-12-16"),
      makeDebit("BCD", "2016-04-05", 50, "2017-01-31"),
      makeDebit("POA1", "2016-04-05", 1100, "2017-01-31"),
      makeDebit("POA2", "2016-04-05", 1100, "2017-01-31")), returns = Some(List(testReturn2016, testReturn2017))), 11)
  )
  val scenariosBasedCustom = Table(("todayDate", "self assessment", "expected answer"),
  (testStartDate.withMonth(12).withDayOfMonth(18), sa.copy(debits
    = List(makeDebit("IN1", "2018-04-05", 30, "2018-01-31"),
    makeDebit("IN2", "2018-04-05", 500, "2018-07-31")), returns = Some(List(testReturn2017.copy(
    receivedDate = Some(LocalDate.of(2017, Month.NOVEMBER, 24))
  ), testReturn2018))), 11))


  //todo ask ela about 11 13 15
  "CalculatorLogic" should {
    TableDrivenPropertyChecks.forAll(scenariosBasedAroundJanuaryDuedate) { (startDate: LocalDate, selfA: SelfAssessment, answer: Int) =>
      s"scenarios Based Around January Due date return the correct number of months($answer) for start date : $startDate and SelfAssessment $selfA" in {
        val result: Int = calculateGapInMonths(selfA, startDate)
        assert(result == answer)

      }
    }
  }

  "CalculatorLogic" should {
    TableDrivenPropertyChecks.forAll(scenariosBasedAroundJulyDuedate) { (startDate: LocalDate, selfA: SelfAssessment, answer: Int) =>
      s"scenarios Based Around July Due date return the correct number of months($answer) for start date : $startDate and SelfAssessment $selfA" in {
        val result: Int = calculateGapInMonths(selfA, startDate)
        assert(result == answer)

      }
    }
  }
  "CalculatorLogic" should {
    TableDrivenPropertyChecks.forAll(scenariosBasedAroundEarlyFilers) { (startDate: LocalDate, selfA: SelfAssessment, answer: Int) =>
      s"scenarios Based Around Early Filers Due date return the correct number of months($answer) for start date : $startDate and SelfAssessment $selfA" in {
        val result: Int = calculateGapInMonths(selfA, startDate)
        assert(result == answer)

      }
    }
  }
  "CalculatorLogic" should {
    TableDrivenPropertyChecks.forAll(scenariosBasedAroundEarlyFilers) { (startDate: LocalDate, selfA: SelfAssessment, answer: Int) =>
      s"scenarios Based Around Late Filers Due date return the correct number of months($answer) for start date : $startDate and SelfAssessment $selfA" in {
        val result: Int = calculateGapInMonths(selfA, startDate)
        assert(result == answer)

      }
    }
  }

    "CalculatorLogic" should {
      TableDrivenPropertyChecks.forAll(scenariosBasedCustom) { (startDate: LocalDate, selfA: SelfAssessment, answer: Int) =>
        s"scenarios Based Around Custom the correct number of months($answer) for start date : $startDate and SelfAssessment $selfA" in {
          val result: Int = calculateGapInMonths(selfA, startDate)
          assert(result == answer)

        }
      }
  }
}
