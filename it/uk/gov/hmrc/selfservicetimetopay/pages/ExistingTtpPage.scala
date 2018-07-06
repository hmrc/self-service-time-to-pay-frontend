package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.WebDriver

object ExistingTtpPage  extends CommonPage {


  val path = "/pay-what-you-owe-in-instalments/eligibility/existing-ttp"

  def assertPageIsDisplayed()(implicit webDriver: WebDriver) = {
    currentPath shouldBe path
    getPageHeader shouldBe "Are you already paying an HMRC debt in instalments?"
  }

}
