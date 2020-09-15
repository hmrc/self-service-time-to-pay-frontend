package ssttpeligibility

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

case class ElibilityServiceConfig(insignificantDebtUpperLimit:                      Int,
                                  maximumDebtForSelfServe:                          Int,
                                  numberOfDaysAfterDueDateForDebtToBeConsideredOld: Int,
                                  returnHistoryYearsRequired:                       Int,
                                  taxYearEndMonthOfYear:                            Int,
                                  taxYearEndDayOfMonth:                             Int)

object ElibilityServiceConfig {
  def fromConfig(servicesConfig: ServicesConfig) = {
    ElibilityServiceConfig(
      insignificantDebtUpperLimit                      = servicesConfig.getInt("eligibility.insignificantDebtUpperLimit"),
      maximumDebtForSelfServe                          = servicesConfig.getInt("eligibility.maximumDebtForSelfServe"),
      numberOfDaysAfterDueDateForDebtToBeConsideredOld = servicesConfig.getInt("eligibility.numberOfDaysAfterDueDateForDebtToBeConsideredOld"),
      returnHistoryYearsRequired                       = servicesConfig.getInt("eligibility.returnHistoryYearsRequired"),
      taxYearEndMonthOfYear                            = servicesConfig.getInt("eligibility.taxYearEndMonthOfYear"),
      taxYearEndDayOfMonth                             = servicesConfig.getInt("eligibility.taxYearEndDayOfMonth")
    )
  }
}