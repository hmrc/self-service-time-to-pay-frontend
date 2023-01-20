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

final case class SpendingData(
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

object SpendingData {
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
  ): SpendingData = {
    SpendingData(
      parseStringToBigDecimal(housingStr),
      parseStringToBigDecimal(pensionContributionStr),
      parseStringToBigDecimal(councilTaxStr),
      parseStringToBigDecimal(utilitiesStr),
      parseStringToBigDecimal(debtRepaymentsStr),
      parseStringToBigDecimal(travelStr),
      parseStringToBigDecimal(childcareStr),
      parseStringToBigDecimal(insuranceStr),
      parseStringToBigDecimal(groceriesStr),
      parseStringToBigDecimal(healthStr),
    )
  }

  private def parseStringToBigDecimal(string: String): BigDecimal = string match {
    case s if s.isEmpty => BigDecimal(0)
    case s              => BigDecimal(s)
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
  //      health:              BigDecimal): SpendingData = {
  //    SpendingData(housing, pensionContribution, councilTax, utilities, debtRepayments, travel, childcare, insurance, groceries, health)
  //  }
  //
  //  implicit def spendingToBigDecimal(inf: SpendingData): (BigDecimal, BigDecimal, BigDecimal, BigDecimal,
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
