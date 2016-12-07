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

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.util.control.Exception.catching

object CalculatorForm {
  val dueByYearMax = 2100
  val dueByYearMin = 2000
  val dueByMonthMin = 1
  val dueByMonthMax = 12
  val dueByDayMin = 1
  val dueByDayMax = 31

  val dueByDateTuple = tuple(
    "dueByDay" -> optional(number)
      .verifying("ssttp.calculator.form.amounts_due.due_by.required-day", _.nonEmpty)
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-day", x => x.isEmpty || ( x.get <= dueByDayMax)|| (x.get >= dueByDayMin)),
    "dueByMonth" -> optional(number)
      .verifying("ssttp.calculator.form.amounts_due.due_by.required-month", _.nonEmpty)
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-month", x => x.isEmpty || ( x.get <= dueByMonthMax)|| (x.get >= dueByMonthMin)),
    "dueByYear" -> optional(number)
      .verifying("ssttp.calculator.form.amounts_due.due_by.required-year", _.nonEmpty)
      .verifying("ssttp.calculator.form.amounts_due.due_by.not-valid-year", x => x.isEmpty || ( x.get <= dueByYearMax)|| (x.get >= dueByYearMin))


  )

  def validDate: Constraint[(Option[Int], Option[Int], Option[Int])] =
    Constraint[(Option[Int], Option[Int], Option[Int])]("ssttp.calculator.form.amounts_due.due_by.not-valid-date") { data =>
    if (data._1.isEmpty || data._2.isEmpty || data._3.isEmpty) {
      Valid
    } else {
      catching(classOf[DateTimeException]).opt(LocalDate.of(data._1.get, data._2.get, data._3.get)) match {
        case d:Some[LocalDate] => Valid
        case _ => Invalid(ValidationError("ssttp.calculator.form.amounts_due.due_by.not-valid-date"))
      }
    }
  }

  val amountDueForm = Form(mapping(
    "amount" -> optional(bigDecimal)
      .verifying("ssttp.calculator.form.amounts_due.amount.required", _.nonEmpty)
      .verifying("ssttp.calculator.form.amounts_due.amount.positive", x => x.isEmpty || (x.get.compare(BigDecimal("0")) >= 0)),
    "dueBy" -> dueByDateTuple.verifying(validDate)
  )
  ((amount:Option[BigDecimal], date:(Option[Int], Option[Int], Option[Int])) =>
    CalculatorAmountDue(amount.get, date._1.get, date._2.get, date._3.get))
  ((amountDue:Debit) => Option((Option(amountDue.amount),
    (Option(amountDue.dueByYear), Option(amountDue.dueByMonth), Option(amountDue.dueByDay)))))
  )

  case class RemoveItem(index: Int)

  val removeAmountDueForm = Form(single("index" -> number))

  def createPaymentTodayForm(totalDue: BigDecimal) = {
    Form(mapping(
      "amount" -> bigDecimal
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-owed", _ < totalDue)
        .verifying("ssttp.calculator.form.payment_today.amount.nonnegitive", _ >= 0)
    )(CalculatorPaymentToday.apply)(CalculatorPaymentToday.unapply))
  }

  val minMonths = 2
  val maxMonths = 11

  val durationForm = Form(mapping("months" -> number(min = minMonths, max = maxMonths))(CalculatorDuration.apply)(CalculatorDuration.unapply))

  // TODO: remove this once CalculatorController code is fully refactored
  def createDurationForm(min: Int, max: Int): Form[CalculatorDuration] = {
    def greaterThan: Constraint[Int] = Constraint[Int]("constraint.duration-more-than") { o =>
      if (o < min) Invalid(ValidationError("ssttp.calculator.form.duration.months.greater-than", min)) else Valid
    }
    def lessThan: Constraint[Int] = Constraint[Int]("constraint.duration-more-than") { o =>
      if (o > max) Invalid(ValidationError("ssttp.calculator.form.duration.months.less-than", max)) else Valid
    }

    Form(mapping("months" -> number.verifying(greaterThan, lessThan))(CalculatorDuration.apply)(CalculatorDuration.unapply))
  }
}
