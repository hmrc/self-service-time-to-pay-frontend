package pagespecs

import langswitch.Languages
import testsupport.ItSpec

class TaxLiabilitiesPageSpec extends ItSpec {

  "language" in {
    taxLiabilitiesPage.open()
    taxLiabilitiesPage.assertPageIsDisplayed()

    taxLiabilitiesPage.clickOnWelshLink()
    taxLiabilitiesPage.assertPageIsDisplayed(Languages.Welsh)

    taxLiabilitiesPage.clickOnEnglishLink()
    taxLiabilitiesPage.assertPageIsDisplayed(Languages.English)
  }
}
