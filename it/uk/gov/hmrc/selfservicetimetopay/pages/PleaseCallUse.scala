package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.WebDriver

object PleaseCallUse extends CommonPage {


  def path(innerPath:String) = s"/pay-what-you-owe-in-instalments/eligibility/${innerPath}/call-us"

  def assertPageIsDisplayed(currantPage:String)(implicit webDriver: WebDriver) = {
    val pathWith =  path(currantPage)
    currentPath shouldBe pathWith
    getPageHeader shouldBe "Please call us"
  }

}

