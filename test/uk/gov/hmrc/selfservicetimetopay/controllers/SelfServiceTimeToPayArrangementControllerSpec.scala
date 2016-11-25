package uk.gov.hmrc.selfservicetimetopay.controllers

import org.joda.time.DateTime
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.CredentialStrength.Weak
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.connectors.{ArrangementConnector, DirectDebitConnector, TaxPayerConnector}

import scala.concurrent.Future

class SelfServiceTimeToPayArrangementControllerSpec extends UnitSpec
                with MockitoSugar with WithFakeApplication with ScalaFutures {


  val authConnector = mock[AuthConnector]
  val ddConnector = mock[DirectDebitConnector]
  val arrangementConnector = mock[ArrangementConnector]
  val taxPayerConnector = mock[TaxPayerConnector]
  val sessionCache = mock[SessionCache]

  val controller = new SelfServiceTimeToPayArrangementController(
    ddConnector,
    arrangementConnector,
    taxPayerConnector,
    authConnector,
    sessionCache
  )



  private def authority(): Authority = {
    val accounts = Accounts(sa = Some(SaAccount("link", SaUtr("utr"))))
    Authority("uri", accounts, None, None, CredentialStrength.None, ConfidenceLevel.L200, None, None, None)
  }

  "Self Service Time To Pay Arrangement Controller" should {
    "return success and display the application complete page" in {

      when(authConnector.currentAuthority).thenReturn(Future.successful(Some(authority())))
      when(taxPayerConnector.getTaxPayer("utr")).thenReturn(Future.successful(Some(taxpayer)))

      val result = controller.submit

    }

  }
}
