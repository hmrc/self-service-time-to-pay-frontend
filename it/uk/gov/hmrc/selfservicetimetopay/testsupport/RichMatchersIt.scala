package uk.gov.hmrc.selfservicetimetopay.testsupport

import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}


trait RichMatchersIt
  extends Matchers
    with DiagrammedAssertions
    with TryValues
    with EitherValues
    with OptionValues
    with AppendedClues
    with ScalaFutures
    with StreamlinedXml
    with Inside
    with Eventually
    with IntegrationPatience



