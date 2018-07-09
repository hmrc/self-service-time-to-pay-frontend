package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.WebDriver

object TaxLiabilitiesPage  extends CommonPage{

  def path(innerPath:String) = s"/pay-what-you-owe-in-instalments/calculator/tax-liabilities"

  def assertPageIsDisplayed(currantPage:String)(implicit webDriver: WebDriver) = {
    val pathWith =  path(currantPage)
    currentPath shouldBe pathWith
    getPageHeader shouldBe "Your tax account summary"
  }
}
