package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.WebDriver

object TypeOfTaxPage extends CommonPage {


  val path = "/pay-what-you-owe-in-instalments/eligibility/type-of-tax"

  def assertPageIsDisplayed()(implicit webDriver: WebDriver) = {
    currentPath shouldBe path
    getPageHeader shouldBe "Is the amount you owe for:"
  }
}
