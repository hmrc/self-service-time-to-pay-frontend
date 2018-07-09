package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.{By, WebDriver}

object TypeOfTaxPage extends CommonPage {


  val path = "/pay-what-you-owe-in-instalments/eligibility/type-of-tax"

  def assertPageIsDisplayed()(implicit webDriver: WebDriver) = {
    currentPath shouldBe path
    getPageHeader shouldBe "Is the amount you owe for:"
  }

  def clickSelfAssessment()(implicit driver: WebDriver): Unit = probing(_.findElement(By.id("type_of_tax_hasSelfAssessmentDebt")).click())
  def clickOtherTypesOfTaxs()(implicit driver: WebDriver): Unit = probing(_.findElement(By.id("type_of_tax_hasOtherDebt")).click())
}
