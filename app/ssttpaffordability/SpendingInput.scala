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

import ssttpaffordability.AffordabilityForm.parseStringToBigDecimal

final case class SpendingInput(
    housing:              BigDecimal,
    pensionContributions: BigDecimal,
    councilTax:           BigDecimal,
    utilities:            BigDecimal,
    debtRepayments:       BigDecimal,
    travel:               BigDecimal,
    childcare:            BigDecimal,
    insurance:            BigDecimal,
    groceries:            BigDecimal,
    health:               BigDecimal
)

object SpendingInput {
  def apply(
      housingStr:              String,
      pensionContributionsStr: String,
      councilTaxStr:           String,
      utilitiesStr:            String,
      debtRepaymentsStr:       String,
      travelStr:               String,
      childcareStr:            String,
      insuranceStr:            String,
      groceriesStr:            String,
      healthStr:               String
  ): SpendingInput = {
    SpendingInput(
      parseStringToBigDecimal(housingStr),
      parseStringToBigDecimal(pensionContributionsStr),
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
    import util.CurrencyUtil.formatToCurrencyString

    Some((
      formatToCurrencyString(spendingInput.housing),
      formatToCurrencyString(spendingInput.pensionContributions),
      formatToCurrencyString(spendingInput.councilTax),
      formatToCurrencyString(spendingInput.utilities),
      formatToCurrencyString(spendingInput.debtRepayments),
      formatToCurrencyString(spendingInput.travel),
      formatToCurrencyString(spendingInput.childcare),
      formatToCurrencyString(spendingInput.insurance),
      formatToCurrencyString(spendingInput.groceries),
      formatToCurrencyString(spendingInput.health)
    ))
  }

}
