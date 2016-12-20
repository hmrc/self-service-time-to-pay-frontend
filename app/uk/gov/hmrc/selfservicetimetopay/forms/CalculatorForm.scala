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

package uk.gov.hmrc.selfservicetimetopay.forms

import java.time.{DateTimeException, LocalDate}
import java.util.Calendar

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.math.BigDecimal.RoundingMode
import scala.util.control.Exception.catching

object CalculatorForm {
  val dueByYearMax = Calendar.getInstance().get(Calendar.YEAR) + 1
  val dueByYearMin = 1996
  val dueByMonthMin = 1
  val dueByMonthMax = 12
  val dueByDayMin = 1
  val dueByDayMax = 31

  def tryToInt(input: String) = {
    catching(classOf[NumberFormatException]) opt input.toInt
  }
  def isInt(input: String) = {
    tryToInt(input).nonEmpty
  }

  val dueByDateTuple = tuple(
    "dueByYear" -> text
      .verifying("ssttp.calculator.form.amounts_due.due_by.required-year", { i: String => (i != null) && i.nonEmpty })
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-year-number", { i => i.isEmpty || (i.nonEmpty && isInt(i)) })
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-year-too-high", { i => !isInt(i) || (isInt(i) && (i.toInt <= dueByYearMax)) })
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-year-too-low", { i => !isInt(i) || (isInt(i) && (i.toInt >= dueByYearMin)) }),
    "dueByMonth" -> optional(number)
      .verifying("ssttp.calculator.form.amounts_due.due_by.required-month", _.nonEmpty)
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-month", x => x.isEmpty || ( x.get <= dueByMonthMax) && (x.get >= dueByMonthMin)),
    "dueByDay" -> text
      .verifying("ssttp.calculator.form.amounts_due.due_by.required-day", { i: String => (i != null) && i.nonEmpty })
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-day-number", { i => i.isEmpty || (i.nonEmpty && isInt(i)) })
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-day", { i => !isInt(i) || (isInt(i) && ( i.toInt <= dueByDayMax) && (i.toInt >= dueByDayMin))})
  )

  def validDate: Constraint[(String, Option[Int], String)] =
    Constraint[(String, Option[Int], String)]("ssttp.calculator.form.amounts_due.due_by.not-valid-date") { data =>
    if (data._1.isEmpty || data._2.isEmpty || data._3.isEmpty) {
      Valid
    } else {
      catching(classOf[DateTimeException]).opt(LocalDate.of(tryToInt(data._1).get, data._2.get, tryToInt(data._3).get)) match {
        case d:Some[LocalDate] => Valid
        case _ => Invalid(ValidationError("ssttp.calculator.form.amounts_due.due_by.not-valid-date"))
      }
    }
  }

  val amountDueForm = Form(mapping(
    "amount" -> optional(bigDecimal)
      .verifying("ssttp.calculator.form.amounts_due.amount.required", _.nonEmpty)
      .verifying("ssttp.calculator.form.amounts_due.amount.positive", x => x.isEmpty || (x.get.compare(BigDecimal("0")) > 0)),
    "dueBy" -> dueByDateTuple.verifying(validDate)
  )
  ((amount:Option[BigDecimal], date:(String, Option[Int], String)) =>
    CalculatorAmountDue(amount.get, tryToInt(date._1).get, date._2.get, tryToInt(date._3).get))
  ((amountDue:Debit) => Option((Option(amountDue.amount),
    (amountDue.dueByYear.toString, Option(amountDue.dueByMonth), amountDue.dueByDay.toString))))
  )

  case class RemoveItem(index: Int)

  val removeAmountDueForm = Form(single("index" -> number))

  def createPaymentTodayForm(totalDue: BigDecimal) = {
    Form(mapping(
      "amount" -> optional(bigDecimal)
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-owed", a => a.isEmpty || a.get.setScale(2, RoundingMode.HALF_UP) < totalDue)
        .verifying("ssttp.calculator.form.payment_today.amount.nonnegitive", a => a.isEmpty || a.get >= 0)
    )(CalculatorPaymentToday.apply)(CalculatorPaymentToday.unapply))
  }

  val minMonths = 2
  val maxMonths = 11

  val durationForm = createDurationForm()

  def createDurationForm(): Form[CalculatorDuration] = {
    def greaterThan: Constraint[Int] = Constraint[Int]("constraint.duration-more-than") { o =>
      if (o < minMonths) Invalid(ValidationError("ssttp.calculator.form.duration.months.greater-than", minMonths)) else Valid
    }
    def lessThan: Constraint[Int] = Constraint[Int]("constraint.duration-more-than") { o =>
      if (o > maxMonths) Invalid(ValidationError("ssttp.calculator.form.duration.months.less-than", maxMonths)) else Valid
    }

    Form(mapping("months" -> number.verifying(greaterThan, lessThan))(CalculatorDuration.apply)(CalculatorDuration.unapply))
  }
}
