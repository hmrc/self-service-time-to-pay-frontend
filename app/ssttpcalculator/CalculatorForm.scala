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
  val MinLeftOverAfterUpfrontPayment = BigDecimal(2)

  def createPaymentTodayForm(totalDue: BigDecimal): Form[CalculatorPaymentTodayForm] = {
    Form(mapping(
      "amount" -> text
        .verifying("ssttp.calculator.form.payment_today.amount.required", { i: String => i.nonEmpty })
        .verifying("ssttp.calculator.form.payment_today.amount.non-numerals",
          { i: String => i.isEmpty | i.matches(CurrencyUtil.regex) })
        .transform[BigDecimal](s => BigDecimal(CurrencyUtil.cleanAmount(s)), CurrencyUtil.formatToCurrencyString)
        .verifying("ssttp.calculator.form.payment_today.amount.decimal-places", { i =>
          i.scale <= 2
        })
        .verifying("ssttp.calculator.form.payment_today.amount.required.min", { i =>
          i >= 1.00
        })
        .verifying(Constraint((i: BigDecimal) =>
          if (totalDue - i >= MinLeftOverAfterUpfrontPayment) Valid
          else {
            Invalid(Seq(ValidationError(
              "ssttp.calculator.form.payment_today.amount.less-than-owed", "£%,1.2f".format(totalDue - MinLeftOverAfterUpfrontPayment).stripSuffix(".00"),
            )))
          }))
    )(CalculatorPaymentTodayForm(_))(bd => Some(bd.amount)))
  }

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

  def apply(maxCustomAmount: BigDecimal)(radioSelection: String, customAmountInput: Option[String]): PlanSelectionRdBttnChoice = {
    if (radioSelection == "cannotAfford") {
      PlanSelectionRdBttnChoice(Left(CannotAfford()))
    } else {
      if (radioSelection == "customAmountOption") {
        PlanSelectionRdBttnChoice(Right(PlanSelection(Right(CustomPlanRequest(customAmountWithSafeMax(customAmountInput, maxCustomAmount))))))
      } else {
        PlanSelectionRdBttnChoice(Right(PlanSelection(Left(SelectedPlan(customSelectionWithSafeMax(radioSelection, maxCustomAmount))))))
      }
    }
  }

  private def customAmountWithSafeMax(customAmountInput: Option[String], maxCustomAmount: BigDecimal): BigDecimal = {
    customAmountInput
      .map(input => { if (BigDecimal(CurrencyUtil.cleanAmount(input)) == maxCustomAmount.setScale(2, HALF_UP)) maxCustomAmount else BigDecimal(CurrencyUtil.cleanAmount(input)) })
      .getOrElse(
        throw new IllegalArgumentException("custom amount option radio selected but no custom amount input found")
      )
  }

  private def customSelectionWithSafeMax(radioSelection: String, maxCustomAmount: BigDecimal): BigDecimal = {
    if (BigDecimal(CurrencyUtil.cleanAmount(radioSelection)).setScale(2, HALF_UP) == maxCustomAmount.setScale(2, HALF_UP)) {
      maxCustomAmount
    } else BigDecimal(CurrencyUtil.cleanAmount(radioSelection))
  }

  def unapply(data: PlanSelectionRdBttnChoice): Option[(String, Option[String])] = Option {
    data.selection match {
      case Left(_) => ("cannotAfford", None)
      case Right(planSelection) => planSelection.selection match {
        case Left(SelectedPlan(instalmentAmount))   => (instalmentAmount.toString, None)
        case Right(CustomPlanRequest(customAmount)) => ("customAmountOption", Some(customAmount.toString()))
      }
    }
  }

  def selectPlanForm(minCustomAmount: BigDecimal = 0, maxCustomAmount: BigDecimal = 0): Form[PlanSelectionRdBttnChoice] =
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

      .verifying(Constraint((i: String) => if ({
        i.nonEmpty && i.matches(CurrencyUtil.regex) && isWithinRange(i, minCustomAmount, maxCustomAmount)
      }) Valid else {
        Invalid(Seq(ValidationError(
          "ssttp.calculator.results.option.other.error.invalid.amount"
        )))
      }))
      .verifying("ssttp.calculator.results.option.other.error.decimal-places", { i =>
        if (Try(BigDecimal(CurrencyUtil.cleanAmount(i))).isSuccess && isWithinRange(i, minCustomAmount, maxCustomAmount)) BigDecimal(CurrencyUtil.cleanAmount(i)).scale <= 2 else true
      })
  }

  private def isWithinRange(amount: String, minCustomAmount: BigDecimal, maxCustomAmount: BigDecimal): Boolean =
    BigDecimal(CurrencyUtil.cleanAmount(amount)) >= minCustomAmount.setScale(2, HALF_UP) &&
      BigDecimal(CurrencyUtil.cleanAmount(amount)) <= maxCustomAmount.setScale(2, HALF_UP)

  def payTodayForm: Form[PayTodayQuestion] = Form(mapping(
    "paytoday" -> optional(boolean).verifying("ssttp.calculator.form.payment_today_question.required", _.nonEmpty)
  )(PayTodayQuestion.apply)(PayTodayQuestion.unapply))

}
