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
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf, mandatoryIfFalse}

import scala.BigDecimal
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

  private val planSelectionFormatter: Formatter[String] = new Formatter[String] {
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

  val planSelectionMapping: Mapping[String] = {
    text
    //    Forms.of[String](planSelectionFormatter)
  }

  val customAmountInputMapping: Mapping[String] = text

  def coerce(radioSelection: String, customAmountInput: Option[String]): PlanSelection = {
    if (radioSelection == "customAmountOption") {
      PlanSelection(Right(CustomPlanRequest(BigDecimal(customAmountInput.getOrElse(
        throw new IllegalArgumentException("custom amount option radio selected but no custom amount input")
      )))))
    } else {
      PlanSelection(Left(SelectedPlan(BigDecimal(radioSelection))))
    }
  }

  def uncoerce(data: PlanSelection): Option[(String, Option[String])] = Option {
    data.selection match {
      case Left(SelectedPlan(instalmentAmount))   => (instalmentAmount.toString, None)
      case Right(CustomPlanRequest(customAmount)) => ("customAmountOption", Some(customAmount.toString()))
    }

  }

  def selectPlanForm(minCustomAmount: BigDecimal, maxCustomAmount: BigDecimal): Form[PlanSelection] =
    Form(mapping(
      "plan-selection" -> planSelectionMapping,
      "custom-amount-input" -> mandatoryIf(
        isEqual("plan-selection", "customAmountOption"),
        validateCustomAmountInput(customAmountInputMapping, minCustomAmount, maxCustomAmount)
      )
    )(coerce)(uncoerce))

  private def validateCustomAmountInput(
      mappingStr:      Mapping[String],
      minCustomAmount: BigDecimal,
      maxCustomAmount: BigDecimal
  ): Mapping[String] = {
    mappingStr
      .verifying("ssttp.calculator.results.option.other.error.no-input", { i: String => i.nonEmpty })
      .verifying("ssttp.calculator.results.option.other.error.non-numeric", { i: String =>
        if (i.nonEmpty) Try(BigDecimal(i)).isSuccess else true
      })
      .verifying("ssttp.calculator.results.option.other.error.below-minimum", { i: String =>
        if (Try(BigDecimal(i)).isSuccess) BigDecimal(i) >= minCustomAmount else true
      })
      .verifying("ssttp.calculator.results.option.other.error.above-maximum", { i: String =>
        if (Try(BigDecimal(i)).isSuccess) BigDecimal(i) <= maxCustomAmount else true
      })
  }

  def payTodayForm: Form[PayTodayQuestion] = Form(mapping(
    "paytoday" -> optional(boolean).verifying("ssttp.calculator.form.payment_today_question.required", _.nonEmpty)
  )(PayTodayQuestion.apply)(PayTodayQuestion.unapply))

}
