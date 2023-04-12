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
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormError, Forms, Mapping}
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}
import util.CurrencyUtil

import scala.math.BigDecimal.RoundingMode.HALF_UP
import scala.util.Try

object CalculatorForm {
  val MaxCurrencyValue: BigDecimal = BigDecimal.exact("1e5")

  def createPaymentTodayForm(totalDue: BigDecimal): Form[CalculatorPaymentTodayForm] =
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.calculator.form.payment_today.amount.required", { i: String => i.nonEmpty })
        .verifying("ssttp.calculator.form.payment_today.amount.non-numerals",
          { i: String => i.isEmpty | i.matches(CurrencyUtil.regex) })
        .verifying("ssttp.calculator.form.payment_today.amount.required.min", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(CurrencyUtil.cleanAmount(i))).isSuccess && BigDecimal(CurrencyUtil.cleanAmount(i)).scale <= 2) BigDecimal(CurrencyUtil.cleanAmount(i)) >= 1.00 else true
        })
        .verifying("ssttp.calculator.form.payment_today.amount.decimal-places", { i =>
          if (Try(BigDecimal(CurrencyUtil.cleanAmount(i))).isSuccess) BigDecimal(CurrencyUtil.cleanAmount(i)).scale <= 2 else true
        })
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-owed", i =>
          if (i.nonEmpty && Try(BigDecimal(CurrencyUtil.cleanAmount(i))).isSuccess) BigDecimal(CurrencyUtil.cleanAmount(i)) < totalDue else true)
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-maxval", { i: String =>
          if (i.nonEmpty && Try(BigDecimal(CurrencyUtil.cleanAmount(i))).isSuccess) BigDecimal(CurrencyUtil.cleanAmount(i)) < MaxCurrencyValue else true
        })
    )(text => CalculatorPaymentTodayForm(CurrencyUtil.cleanAmount(text)))(bd => Some(bd.amount.toString)))

  private val planSelectionFormatter: Formatter[String] = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      val amount = data.get(key) match {
        case None => data.get(key + ".value")
        case x    => x
      }

      amount match {
        case Some(customAmountOption) if Try(customAmountOption == "customAmountOption").isSuccess => Right(customAmountOption)
        case Some(value) if Try(BigDecimal(value)).isSuccess => Right(value)
        case _ => Left(Seq(FormError(key, "ssttp.calculator.results.option.error.no-selection")))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = Map(key + ".value" -> value)
  }

  val planSelectionMapping: Mapping[String] = {
    Forms.of[String](planSelectionFormatter)
  }

  val customAmountInputMapping: Mapping[String] = text

  def apply(maxCustomAmount: BigDecimal)(radioSelection: String, customAmountInput: Option[String]): PlanSelection = {
    if (radioSelection == "customAmountOption") {
      PlanSelection(Right(CustomPlanRequest(customAmountWithSafeMax(customAmountInput, maxCustomAmount))))
    } else {
      PlanSelection(Left(SelectedPlan(customSelectionWithSafeMax(radioSelection, maxCustomAmount))))
    }
  }

  private def customAmountWithSafeMax(customAmountInput: Option[String], maxCustomAmount: BigDecimal): BigDecimal = {

    customAmountInput
      .map(input => { if (BigDecimal(input) == maxCustomAmount.setScale(2, HALF_UP)) maxCustomAmount else BigDecimal(input) })
      .getOrElse(
        throw new IllegalArgumentException("custom amount option radio selected but no custom amount input found")
      )
  }

  private def customSelectionWithSafeMax(radioSelection: String, maxCustomAmount: BigDecimal): BigDecimal = {
    if (BigDecimal(radioSelection).setScale(2, HALF_UP) == maxCustomAmount.setScale(2, HALF_UP)) {
      maxCustomAmount
    } else BigDecimal(radioSelection)
  }

  def unapply(data: PlanSelection): Option[(String, Option[String])] = Option {
    data.selection match {
      case Left(SelectedPlan(instalmentAmount))   => (instalmentAmount.toString, None)
      case Right(CustomPlanRequest(customAmount)) => ("customAmountOption", Some(customAmount.toString()))
    }
  }

  def selectPlanForm(minCustomAmount: BigDecimal = 0, maxCustomAmount: BigDecimal = 0): Form[PlanSelection] =
    Form(mapping(
      "plan-selection" -> planSelectionMapping,
      "custom-amount-input" -> mandatoryIf(
        isEqual("plan-selection", "customAmountOption"),
        validateCustomAmountInput(customAmountInputMapping, minCustomAmount, maxCustomAmount)
      )
    )(apply(maxCustomAmount))(unapply))

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
      .verifying("ssttp.calculator.results.option.other.error.decimal-places", { i =>
        if (Try(BigDecimal(i)).isSuccess) BigDecimal(i).scale <= 2 else true
      })
      .verifying("ssttp.calculator.results.option.other.error.negative-amount", { i: String =>
        if (Try(BigDecimal(i)).isSuccess) BigDecimal(i) >= 0 else true
      })
      .verifying(Constraint((i: String) => if ({
        if (Try(BigDecimal(i)).isSuccess) BigDecimal(i) < 0 || BigDecimal(i) >= minCustomAmount.setScale(2, HALF_UP) else true
      }) Valid else {
        Invalid(Seq(ValidationError(
          "ssttp.calculator.results.option.other.error.below-minimum",
          minCustomAmount.formatted("%,1.2f").stripSuffix(".00"),
          maxCustomAmount.formatted("%,1.2f").stripSuffix(".00")
        )))
      }))
      .verifying(Constraint((i: String) => if ({
        if (Try(BigDecimal(i)).isSuccess) BigDecimal(i) <= maxCustomAmount.setScale(2, HALF_UP) else true
      }) Valid else {
        Invalid(Seq(ValidationError(
          "ssttp.calculator.results.option.other.error.above-maximum",
          minCustomAmount.formatted("%,1.2f").stripSuffix(".00"),
          maxCustomAmount.formatted("%,1.2f").stripSuffix(".00")
        )))
      }))

  }

  def payTodayForm: Form[PayTodayQuestion] = Form(mapping(
    "paytoday" -> optional(boolean).verifying("ssttp.calculator.form.payment_today_question.required", _.nonEmpty)
  )(PayTodayQuestion.apply)(PayTodayQuestion.unapply))

}
