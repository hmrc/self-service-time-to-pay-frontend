package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.{By, WebDriver}

object StartPage extends CommonPage {
  val path = "/pay-what-you-owe-in-instalments"

  def assertPageIsDisplayed()(implicit webDriver: WebDriver) = {
    currentPath shouldBe path
    getPageHeader shouldBe "Pay what you owe in instalments"
  }

 override def getPageHeader(implicit driver: WebDriver): String = probing(_.findElement(By.className("h1-heading")).getText)

  def clickStart()(implicit driver: WebDriver): Unit = probing(_.findElement(By.className("button--get-started")).click())
}
