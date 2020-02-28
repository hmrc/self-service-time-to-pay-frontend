package testsupport.testdata

import timetopaytaxpayer.cor.model.{Address, Debit, Return, SelfAssessmentDetails, Taxpayer}

class EligibilityTaxpayerVariationsTd {

  val taxpayerName: String = "The Emperor Of Mankind"
  val taxpayerAddress: Address = Address(Some("Golden Throne"), Some("Himalayan Mountains"), Some("Holy Terra"), Some("Segmentum Solar"),
    Some("Milky Way Galaxy"), Some("BN11 1XX"))

  def initTaxpayer(selfAssessmentDetails: SelfAssessmentDetails): Taxpayer = Taxpayer(taxpayerName, Seq(taxpayerAddress), selfAssessmentDetails)

  def initSelfAssessmentDetails(debits: Seq[Debit], returns: Seq[Return]) = SelfAssessmentDetails(TdAll.Sautr, TdAll.communicationPreferences, debits, returns)

  val

}
