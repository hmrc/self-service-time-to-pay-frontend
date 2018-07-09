package uk.gov.hmrc.selfservicetimetopay.pages

import org.openqa.selenium.WebDriver
object SignInQuestion extends CommonPage {


    val path = "/pay-what-you-owe-in-instalments/eligibility/sign-in-question"

    def assertPageIsDisplayed()(implicit webDriver: WebDriver) = {
      currentPath shouldBe path
      getPageHeader shouldBe "Would you like to sign in and view what you owe?"
    }

}
