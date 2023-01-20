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

final case class SpendingInput(
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

object SpendingInput {
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
  ): SpendingInput = {
    SpendingInput(
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

  def unapply(spendingInput: SpendingInput): Option[(String, String, String, String, String, String, String, String, String, String)] = {
    Some((
      spendingInput.housing.toString(),
      spendingInput.pensionContribution.toString(),
      spendingInput.councilTax.toString(),
      spendingInput.utilities.toString(),
      spendingInput.debtRepayments.toString(),
      spendingInput.travel.toString(),
      spendingInput.childcare.toString(),
      spendingInput.insurance.toString(),
      spendingInput.groceries.toString(),
      spendingInput.health.toString()
    ))
  }

  private def parseStringToBigDecimal(string: String): BigDecimal = string match {
    case s if s.isEmpty => BigDecimal(0)
    case s              => BigDecimal(s)
  }

}
