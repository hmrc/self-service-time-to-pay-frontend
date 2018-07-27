package uk.gov.hmrc.selfservicetimetopay.tests

import uk.gov.hmrc.selfservicetimetopay.pages._
import uk.gov.hmrc.selfservicetimetopay.testsupport.BrowserSpec

class ExistingTtpPageSpec extends BrowserSpec{

  val page = ExistingTtpPage

/*  "Load the page up" in new TestSetup {
    page.assertPageIsDisplayed()
  }

  "go back to the type of tax page" in new TestSetup {
    page.clickBack()
    TypeOfTaxPage.assertPageIsDisplayed()
  }

  "show an error if noting is selected" in new TestSetup {
    page.clickContinue()
    page.assertPageIsDisplayed()
    page.assertFormSummaryBoxIsDisplayed()
  }

  "show the please call use page is the user clicks yes" in new TestSetup {
    page.clickYes()
    page.clickContinue()
    PleaseCallUse.assertPageIsDisplayed("existing-ttp")
  }
  "show the would you like to sign in page if the user is not signed in and clicks no" in new TestSetup {
    page.clickNo()
    page.clickContinue()
    SignInQuestion.assertPageIsDisplayed()
  }
//todo nb do signed in journeys no time now as it will require a lot of work
  //add in all pages and do stubs
*/

  trait TestSetup {
    goTo(StartPage.path)
    StartPage.clickStart()
    TypeOfTaxPage.clickSelfAssessment()
    TypeOfTaxPage.clickContinue()
  }

}
