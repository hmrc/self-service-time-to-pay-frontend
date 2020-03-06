/*
 * Copyright 2020 HM Revenue & Customs
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

package testsupport.testdata

import java.time.LocalDate

import timetopaytaxpayer.cor.model.{Address, Debit, Interest, Return, SaUtr, SelfAssessmentDetails, Taxpayer}

object EligibilityTaxpayerVariationsTd {
  val dummyCurrentDate: LocalDate = LocalDate.of(2019, 11, 1)
  val dummyTaxYearEnd: LocalDate = LocalDate.of(2020, 4, 5)
  val dummy60DaysAgo: LocalDate = dummyCurrentDate.minusDays(60)
  val zeroInterestOption: Option[Interest] = Some(Interest(Some(dummyCurrentDate), BigDecimal(0)))
  val taxpayerName: String = "The Emperor Of Mankind"
  val taxpayerAddress: Address = Address(Some("Golden Throne"), Some("Himalayan Mountains"), Some("Holy Terra"), Some("Segmentum Solar"),
                                         Some("Milky Way Galaxy"), Some("BN11 1XX"))

  def initDebit(originCode: String, amount: Double, dueDate: LocalDate): Debit = Debit(originCode, BigDecimal(amount), Some(dueDate),
                                                                                       zeroInterestOption, dummyTaxYearEnd)

  def initEligibleDebit(): Debit = initDebit("IN1", 33, dummyCurrentDate)

  def initSelfAssessmentDetails(debits: Seq[Debit], returns: Seq[Return]): SelfAssessmentDetails = SelfAssessmentDetails(TdAll.Sautr, TdAll.communicationPreferences, debits, returns)

  def initSelfAssessmentDetailsWithErroneousUtr(debits: Seq[Debit], returns: Seq[Return]): SelfAssessmentDetails = SelfAssessmentDetails(SaUtr("XXXXXXXXXX"), TdAll.communicationPreferences, debits, returns)

  def initTaxpayer(debits: Seq[Debit], returns: Seq[Return]): Taxpayer = Taxpayer(taxpayerName, Seq(taxpayerAddress), initSelfAssessmentDetails(debits, returns))

  def initTaxpayerWithErroneousUtr(debits: Seq[Debit], returns: Seq[Return]): Taxpayer = Taxpayer(taxpayerName, Seq(taxpayerAddress), initSelfAssessmentDetailsWithErroneousUtr(debits, returns))

  val zeroDebtTaxpayer: Taxpayer = initTaxpayer(Seq.empty, Seq.empty)

  val insignificantDebtTaxpayer: Taxpayer = initTaxpayer(Seq(initDebit("IN1", 31, dummyCurrentDate)), Seq.empty)

  val oldDebtIsTooHighTaxpayer: Taxpayer = initTaxpayer(Seq(initDebit("IN1", 33, dummy60DaysAgo)), Seq.empty)

  val totalDebtIsTooHighTaxpayer: Taxpayer = initTaxpayer(Seq(initDebit("IN1", 10000, dummyCurrentDate)), Seq.empty)

  val returnNeedsSubmittingTaxpayer: Taxpayer = initTaxpayer(Seq(initEligibleDebit), Seq(Return(dummyTaxYearEnd, Some(dummy60DaysAgo), Some(dummyCurrentDate), None)))

  val notOnIaTaxpayer: Taxpayer = initTaxpayer(Seq(initEligibleDebit), Seq.empty)
}
