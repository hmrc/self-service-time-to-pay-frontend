package uk.gov.hmrc.selfservicetimetopay.tests

import uk.gov.hmrc.selfservicetimetopay.pages.{ExistingTtpPage, PleaseCallUse, StartPage, TypeOfTaxPage}
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


  "show the please call use page if Other types of tax is selected " in new TestSetup {
    page.clickOtherTypesOfTaxs()
    page.clickContinue()
    PleaseCallUse.assertPageIsDisplayed("type-of-tax")
  }

  "show the please call use page if Other types of tax is selected as well as self assessment  " in new TestSetup {
    page.clickOtherTypesOfTaxs()
    page.clickSelfAssessment()
    page.clickContinue()
    PleaseCallUse.assertPageIsDisplayed("type-of-tax")
  }

  "show the existing ttp page if self assessment is selected" in new TestSetup {
    page.clickSelfAssessment()
    page.clickContinue()
    ExistingTtpPage.assertPageIsDisplayed()
  }


  trait TestSetup {
    goTo(StartPage.path)
    StartPage.clickStart()
    goTo(page.path)
  }

}

