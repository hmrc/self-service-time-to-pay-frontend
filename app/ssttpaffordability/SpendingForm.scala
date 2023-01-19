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

package ssttpaffordability

import scala.math.BigDecimal.RoundingMode.HALF_UP

final case class SpendingForm(
    housing:             BigDecimal,
    pensionContribution: BigDecimal,
    councilTax:          BigDecimal,
    utilities:           BigDecimal,
    debtRepayments:      BigDecimal,
    travel:              BigDecimal,
    childcare:           BigDecimal,
    insurance:           BigDecimal,
    groceries:           BigDecimal,
    health:              BigDecimal
)

object SpendingForm {
  def apply(
      housingStr:             String,
      pensionContributionStr: String,
      councilTaxStr:          String,
      utilitiesStr:           String,
      debtRepaymentsStr:      String,
      travelStr:              String,
      childcareStr:           String,
      insuranceStr:           String,
      groceriesStr:           String,
      healthStr:              String
  ): SpendingForm = {
    val housingBigDecimal = housingStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val pensionContributionBigDecimal = pensionContributionStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val councilTaxBigDecimal = councilTaxStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val utilitiesBigDecimal = utilitiesStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val debtRepaymentsBigDecimal = debtRepaymentsStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val travelBigDecimal = travelStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val childcareBigDecimal = childcareStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val insuranceBigDecimal = insuranceStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val groceriesBigDecimal = groceriesStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val healthBigDecimal = healthStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    SpendingForm(housingBigDecimal, pensionContributionBigDecimal, councilTaxBigDecimal,
                 utilitiesBigDecimal, debtRepaymentsBigDecimal, travelBigDecimal, childcareBigDecimal,
                 insuranceBigDecimal, groceriesBigDecimal, healthBigDecimal)
  }

  //  implicit def bigDecimalsToSpending(
  //      housing:             BigDecimal,
  //      pensionContribution: BigDecimal,
  //      councilTax:          BigDecimal,
  //      utilities:           BigDecimal,
  //      debtRepayments:      BigDecimal,
  //      travel:              BigDecimal,
  //      childcare:           BigDecimal,
  //      insurance:           BigDecimal,
  //      groceries:           BigDecimal,
  //      health:              BigDecimal): SpendingForm = {
  //    SpendingForm(housing, pensionContribution, councilTax, utilities, debtRepayments, travel, childcare, insurance, groceries, health)
  //  }
  //
  //  implicit def spendingToBigDecimal(inf: SpendingForm): (BigDecimal, BigDecimal, BigDecimal, BigDecimal,
  //    BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal) = {
  //    (
  //      inf.housing.setScale(2, HALF_UP),
  //      inf.pensionContribution.setScale(2, HALF_UP),
  //      inf.councilTax.setScale(2, HALF_UP),
  //      inf.utilities.setScale(2, HALF_UP),
  //      inf.debtRepayments.setScale(2, HALF_UP),
  //      inf.travel.setScale(2, HALF_UP),
  //      inf.childcare.setScale(2, HALF_UP),
  //      inf.insurance.setScale(2, HALF_UP),
  //      inf.groceries.setScale(2, HALF_UP),
  //      inf.health.setScale(2, HALF_UP)
  //    )
  //  }
  //
  //  def unapply(arg: BigDecimal): Option[BigDecimal] = Option(arg.setScale(2, HALF_UP))
}
