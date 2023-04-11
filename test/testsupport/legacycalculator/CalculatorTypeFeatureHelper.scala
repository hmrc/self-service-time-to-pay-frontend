
package testsupport.legacycalculator

import com.softwaremill.macwire.wire
import pagespecs.pages.legacycalculator.HowMuchCanYouPayEachMonthLegacyPage
import testsupport.ItSpec

trait CalculatorTypeFeatureHelper extends ItSpec {
  lazy val howMuchCanYouPayEachMonthLegacyPage: HowMuchCanYouPayEachMonthLegacyPage = wire[HowMuchCanYouPayEachMonthLegacyPage]
}
