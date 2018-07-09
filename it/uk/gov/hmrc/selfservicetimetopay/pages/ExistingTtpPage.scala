package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.{By, WebDriver}

object ExistingTtpPage  extends CommonPage {


  val path = "/pay-what-you-owe-in-instalments/eligibility/existing-ttp"

  def assertPageIsDisplayed()(implicit webDriver: WebDriver) = {
    currentPath shouldBe path
    getPageHeader shouldBe "Are you already paying an HMRC debt in instalments?"
  }

  def clickYes()(implicit webDriver: WebDriver) =  probing(_.findElement(By.id("radio-inline-1")).click())

  def clickNo()(implicit webDriver: WebDriver) =  probing(_.findElement(By.id("radio-inline-2")).click())
}
