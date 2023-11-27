
package pagespecs.pages

import langswitch.{Language, Languages}
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import org.scalatestplus.selenium.WebBrowser.pageTitle
import testsupport.RichMatchers.convertToAnyShouldWrapper

class DeleteAnswersPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  override def path: String = "/pay-what-you-owe-in-instalments/delete-answers"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "For your security, we signed you out"
    case Languages.Welsh => "Er eich diogelwch, gwnaethom eich allgofnodi"
  }

  def assertHasSignInButton(language: Language = English): Assertion = probing {
    val signInButton = WebBrowser.find(WebBrowser.ClassNameQuery("govuk-button"))
    val signInText = language match {
      case English => "Sign in"
      case Welsh => "Mewngofnodi"
    }
    val signInRedirectUrl = baseUrl.value + controllers.routes.TimeoutController.signInAgain.url
    signInButton.map(e => e.text) shouldBe Some(signInText)
    signInButton.flatMap(e => e.attribute("href")) shouldBe Some(signInRedirectUrl)
  }

  object Expected {
    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish = """Set up a Self Assessment payment plan"""

      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {

      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh => mainTextWelsh
      }

      private val mainTextEnglish = "We did not save your answers."

      private val mainTextWelsh = "Ni wnaethom gadw’ch atebion."

    }
  }


}
