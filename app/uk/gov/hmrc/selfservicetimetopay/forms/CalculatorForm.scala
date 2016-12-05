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

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.selfservicetimetopay.models._

object CalculatorForm {
  val dueByYearMax = 2100
  val dueByYearMin = 2000
  val dueByMonthMin = 1
  val dueByMonthMax = 12
  val dueByDayMin = 1
  val dueByDayMax = 31

  val amountDueForm = Form(mapping(
    "amount" -> bigDecimal,
    "dueByYear" -> number(min = dueByYearMin, max = dueByYearMax),
    "dueByMonth" -> number(min = dueByDayMin, max = dueByMonthMax),
    "dueByDay" -> number(min = dueByDayMin, max = dueByDayMax)
  )(CalculatorAmountDue.apply)(CalculatorAmountDue.unapply))

  case class RemoveItem(index: Int)

  val removeAmountDueForm = Form(single("index" -> number))

  val paymentTodayForm = Form(mapping(
    "amount" -> bigDecimal
      .verifying("ssttp.calculator.form.payment_today.amount.nonnegitive", _.compare(BigDecimal.valueOf(0)) >= 0)
  )(CalculatorPaymentToday.apply)(CalculatorPaymentToday.unapply))

  def createPaymentTodayForm(totalDue: BigDecimal): Form[BigDecimal] = {
    Form(mapping(
      "amount" -> bigDecimal
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-owed", _ < totalDue)
        .verifying("ssttp.calculator.form.payment_today.amount.nonnegitive", _.compare(BigDecimal("0")) >= 0)
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
