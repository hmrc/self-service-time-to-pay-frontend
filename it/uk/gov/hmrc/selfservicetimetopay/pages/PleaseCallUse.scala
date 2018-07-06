package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.WebDriver

object PleaseCallUse extends CommonPage {


  val path = "/pay-what-you-owe-in-instalments/eligibility/type-of-tax/call-us"

  def assertPageIsDisplayed()(implicit webDriver: WebDriver) = {
    currentPath shouldBe path
    getPageHeader shouldBe "Please call us"
  }

}
