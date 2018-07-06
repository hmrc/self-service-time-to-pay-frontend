package uk.gov.hmrc.selfservicetimetopay.tests

import uk.gov.hmrc.selfservicetimetopay.pages.{StartPage, TypeOfTaxPage}
import uk.gov.hmrc.selfservicetimetopay.testsupport.BrowserSpec

class TypeOfTaxPageSpec extends BrowserSpec {

  val page = TypeOfTaxPage

  "Load the page up" in new TestSetup {
    page.assertPageIsDisplayed()
  }

  "click back to the previous page in" in new TestSetup {
    page.clickBack()
    StartPage.assertPageIsDisplayed()
  }

  "show an error if nothing is selected when continue is clicked " in new TestSetup {
    page.clickContinue()
    page.assertPageIsDisplayed()
    page.assertFormSummaryBoxIsDisplayed()
  }

  trait TestSetup {
    goTo(StartPage.path)
    StartPage.clickStart()
    goTo(page.path)
  }

}

