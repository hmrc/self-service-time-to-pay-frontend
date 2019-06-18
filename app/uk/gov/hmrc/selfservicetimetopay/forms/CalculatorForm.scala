/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.data.{Form, FormError, Forms, Mapping}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import uk.gov.hmrc.selfservicetimetopay.models.CalculatorAmountDue.MaxCurrencyValue
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.util.{Success, Try}
import scala.util.control.Exception.catching

object CalculatorForm {

  def tryToInt(input: String) = {
    catching(classOf[NumberFormatException]) opt input.toInt
  }

  def isInt(input: String) = {
    tryToInt(input).nonEmpty
  }

  def hasValue(textBox: String): Boolean = {
    (textBox != null) && textBox.nonEmpty
  }

  def createPaymentTodayForm(totalDue: BigDecimal): Form[CalculatorPaymentTodayForm] = {
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.calculator.form.payment_today.amount.required.min", { i: String => if (i.nonEmpty && Try(BigDecimal(i)).isSuccess && BigDecimal(i).scale <= 2) BigDecimal(i) >= 0.00 else true })
        .verifying("ssttp.calculator.form.payment_today.amount.required", { i => Try(BigDecimal(i)).isSuccess })
        .verifying("ssttp.calculator.form.payment_today.amount.decimal-places", { i =>
          if (Try(BigDecimal(i)).isSuccess) BigDecimal(i).scale <= 2 else true
        })
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-owed", i =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess) BigDecimal(i) < totalDue else true)
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-maxval", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess) BigDecimal(i) < MaxCurrencyValue else true
        })
    )(text => CalculatorPaymentTodayForm(text))(bd => Some(bd.amount.toString)))
  }

  def createAmountDueForm(): Form[CalculatorSinglePayment] = {
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.calculator.form.amount-due.required", { i: String => Try(BigDecimal(i)).isSuccess })
        .verifying("ssttp.calculator.form.amount-due.required.min", { i: String => if (i.nonEmpty && Try(BigDecimal(i)).isSuccess && BigDecimal(i).scale <= 2) BigDecimal(i) >= 32.00 else true })
        .verifying("ssttp.calculator.form.amount-due.less-than-maxval", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess) BigDecimal(i) < MaxCurrencyValue else true
        })
        .verifying("ssttp.calculator.form.amount-due.decimal-places", { i =>
          if (Try(BigDecimal(i)).isSuccess) BigDecimal(i).scale <= 2 else true
        })
    )(text => CalculatorSinglePayment(text))(bd => Some(bd.amount.toString)))
  }

  def createMonthlyAmountForm(min: Int, max: Int): Form[MonthlyAmountForm] = {
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.monthly.amount.numbers-only", { i: String => Try(BigDecimal(i)).isSuccess })
        .verifying("ssttp.monthly.amount.out-of-bounds", { i: String => Try(BigDecimal(i)).isFailure || BigDecimal(i) >= min && BigDecimal(i) <= max })) (text => MonthlyAmountForm(text))(bd => Some(bd.amount.toString)))

  }

  private val chosenMonthFormatter: Formatter[String] = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      val month = data.get(key) match {
        case None => data.get(key + ".value")
        case x    => x
      }

      //todo add in values for max allowed months in here
      month match {
        case Some(value) if Try(BigDecimal(value)).isSuccess => Right(value)
        case _ => Left(Seq(FormError(key, "ssttp.calculator.results.month.required")))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = Map(key + ".value" -> value.toString)
  }

  val chosenMonthMapping: Mapping[String] = Forms.of[String](chosenMonthFormatter)

  def createInstalmentForm(): Form[CalculatorDuration] = {
    Form(mapping("chosen-month" -> chosenMonthMapping)(text => CalculatorDuration(text.toInt))(_ => Some(text.toString)))
  }

  def payTodayForm: Form[PayTodayQuestion] = Form(mapping(
    "paytoday" -> optional(boolean).verifying("ssttp.calculator.form.payment_today_question.required", _.nonEmpty)
  )(PayTodayQuestion.apply)(PayTodayQuestion.unapply))

}
