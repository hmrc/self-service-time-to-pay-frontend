
package pagespecs

import langswitch.Languages.Welsh
import org.openqa.selenium.By.className
import org.scalatestplus.selenium.WebBrowser
import testsupport.ItSpec

class DeleteAnswersPageSpec extends ItSpec {

  "language" in {
    deleteAnswersPage.open()
    deleteAnswersPage.assertInitialPageIsDisplayed

    deleteAnswersPage.clickOnWelshLink()
    deleteAnswersPage.assertInitialPageIsDisplayed(Welsh)
  }

  "sign in button" in {
    deleteAnswersPage.open()
    deleteAnswersPage.assertHasSignInButton()

    deleteAnswersPage.clickOnWelshLink()
    deleteAnswersPage.assertHasSignInButton(Welsh)
  }

  "no back button" in {
    deleteAnswersPage.open()
    deleteAnswersPage.hasNoBackLink
  }

}
