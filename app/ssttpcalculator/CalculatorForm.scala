/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.Forms.{text, _}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms, Mapping}
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.voa.play.form.Condition
import uk.gov.voa.play.form.ConditionalMappings.{mandatoryIf, mandatoryIfFalse}

import scala.util.Try

object CalculatorForm {
  val MaxCurrencyValue: BigDecimal = BigDecimal.exact("1e5")

  def createPaymentTodayForm(totalDue: BigDecimal): Form[CalculatorPaymentTodayForm] =
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.calculator.form.payment_today.amount.required", { i: String => i.nonEmpty })
        .verifying("ssttp.calculator.form.payment_today.amount.non-numerals", { i: String =>
          if (i.nonEmpty) Try(BigDecimal(i)).isSuccess else true
        })
        .verifying("ssttp.calculator.form.payment_today.amount.required.min", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess && BigDecimal(i).scale <= 2) BigDecimal(i) >= 1.00 else true
        })
        .verifying("ssttp.calculator.form.payment_today.amount.decimal-places", { i =>
          if (Try(BigDecimal(i)).isSuccess) BigDecimal(i).scale <= 2 else true
        })
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-owed", i =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess) BigDecimal(i) < totalDue else true)
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-maxval", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess) BigDecimal(i) < MaxCurrencyValue else true
        })
    )(text => CalculatorPaymentTodayForm(text))(bd => Some(bd.amount.toString)))

  def createAmountDueForm(): Form[CalculatorSinglePayment] =
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.calculator.form.amount-due.required", { i: String => Try(BigDecimal(i)).isSuccess })
        .verifying("ssttp.calculator.form.amount-due.required.min", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess && BigDecimal(i).scale <= 2) BigDecimal(i) >= 32.00 else true
        })
        .verifying("ssttp.calculator.form.amount-due.less-than-maxval", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(i)).isSuccess) BigDecimal(i) < MaxCurrencyValue else true
        })
        .verifying("ssttp.calculator.form.amount-due.decimal-places", { i =>
          if (Try(BigDecimal(i)).isSuccess) BigDecimal(i).scale <= 2 else true
        })
    )(text => CalculatorSinglePayment(text))(bd => Some(bd.amount.toString)))

  def createMonthlyAmountForm(min: Int, max: Int): Form[MonthlyAmountForm] =
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.monthly.amount.numbers-only", { i: String => Try(BigDecimal(i)).isSuccess })
        .verifying("ssttp.monthly.amount.out-of-bounds", { i: String =>
          Try(BigDecimal(i)).isFailure || BigDecimal(i) >= min && BigDecimal(i) <= max
        }))(text => MonthlyAmountForm(text))(bd => Some(bd.amount.toString)))

  private val selectedPlanAmountFormatter: Formatter[String] = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      val amount = data.get(key) match {
        case None => data.get(key + ".value")
        case x    => x
      }

      amount match {
        case Some(value) if Try(BigDecimal(value)).isSuccess => Right(value)
        case _ => Left(Seq(FormError(key, "ssttp.calculator.results.amount.required")))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = Map(key + ".value" -> value.toString)
  }

  val selectedPlanAmountMapping: Mapping[String] = Forms.of[String](selectedPlanAmountFormatter)

  val customAmountInputMapping: Mapping[String] = text

  def coerce(selectedPlanAmount: Option[String], customAmountInput: Option[String]): PlanSelection = {
    PlanSelection(selectedPlanAmount.map(BigDecimal(_)), customAmountInput.map(BigDecimal(_))
    )
  }

  def uncoerce(data: PlanSelection): Option[(Option[String], Option[String])] = Option {
    (data.selectedPlanAmount.map(_.toString), data.customAmountInput.map(_.toString))
  }

  def selectPlanForm(): Form[PlanSelection] =
    Form(mapping(
      "selected-plan-amount" -> optional(selectedPlanAmountMapping),
      "customAmountInput" -> optional(customAmountInputMapping)
    )(coerce)(uncoerce))

  def payTodayForm: Form[PayTodayQuestion] = Form(mapping(
    "paytoday" -> optional(boolean).verifying("ssttp.calculator.form.payment_today_question.required", _.nonEmpty)
  )(PayTodayQuestion.apply)(PayTodayQuestion.unapply))

}
