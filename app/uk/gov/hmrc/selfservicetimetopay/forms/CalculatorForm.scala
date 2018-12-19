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

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.selfservicetimetopay.models.CalculatorAmountDue.MaxCurrencyValue
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.util.Try
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


  case class RemoveItem(index: Int)

  val removeAmountDueForm = Form(single("index" -> number))

  def createPaymentTodayForm(totalDue: BigDecimal): Form[CalculatorPaymentToday] = {
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
    )(text => CalculatorPaymentToday(text))(bd => Some(bd.amount.toString)))
  }

  def createSinglePaymentForm(): Form[CalculatorSinglePayment] = {
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.calculator.form.amount-due.required", { i: String => Try(BigDecimal(i)).isSuccess })
        .verifying("ssttp.calculator.form.amount-due.required.min", { i: String => if (i.nonEmpty && Try(BigDecimal(i)).isSuccess && BigDecimal(i).scale <= 2) BigDecimal(i) >= 0.01 else true })
        .verifying("ssttp.calculator.form.amount-due.less-than-maxval", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess) BigDecimal(i) < MaxCurrencyValue else true
        })
        .verifying("ssttp.calculator.form.amount-due.decimal-places", { i =>
          if (Try(BigDecimal(i)).isSuccess) BigDecimal(i).scale <= 2 else true
        })
    )(text => CalculatorSinglePayment(text))(bd => Some(bd.amount.toString)))
  }

  //todo add in values for max allowed months in here
  def createInstalmentForm(): Form[CalculatorDuration] = {
    Form(mapping(
      "chosen_month" -> text
        .verifying("ssttp.calculator.results.month.required", { i: String => Try(BigDecimal(i)).isSuccess })
  )(text => CalculatorDuration(Some(text.toInt)))(_ => Some(text.toString)))
}

  def payTodayForm: Form[PayTodayQuestion] = Form(mapping(
    "paytoday" -> optional(boolean).verifying("ssttp.calculator.form.payment_today_question.required", _.nonEmpty)
  )(PayTodayQuestion.apply)(PayTodayQuestion.unapply))

}
