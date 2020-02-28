package testsupport.testdata

import java.time.LocalDate

import timetopaytaxpayer.cor.model.{Address, Debit, Interest, Return, SelfAssessmentDetails, Taxpayer}

object EligibilityTaxpayerVariationsTd {
  val dummyCurrentDate: LocalDate = LocalDate.of(2019, 5, 1)
  val dummyTaxYearEnd: LocalDate = LocalDate.of(2020, 4, 5)
  val zeroInterestOption: Option[Interest] = Some(Interest(Some(dummyCurrentDate), BigDecimal(0)))
  val taxpayerName: String = "The Emperor Of Mankind"
  val taxpayerAddress: Address = Address(Some("Golden Throne"), Some("Himalayan Mountains"), Some("Holy Terra"), Some("Segmentum Solar"),
    Some("Milky Way Galaxy"), Some("BN11 1XX"))

  def initTaxpayer(selfAssessmentDetails: SelfAssessmentDetails): Taxpayer = Taxpayer(taxpayerName, Seq(taxpayerAddress), selfAssessmentDetails)

  def initSelfAssessmentDetails(debits: Seq[Debit], returns: Seq[Return]) = SelfAssessmentDetails(TdAll.Sautr, TdAll.communicationPreferences, debits, returns)

  def initDebit(originCode: String, amount: Double, dueDate: LocalDate): Debit = Debit(originCode, BigDecimal(amount), dueDate,
    zeroInterestOption, dummyTaxYearEnd)

  val zeroDebtTaxpayer: Taxpayer = initTaxpayer(initSelfAssessmentDetails(Seq.empty, Seq.empty))


}
