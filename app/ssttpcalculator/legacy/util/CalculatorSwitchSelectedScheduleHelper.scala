/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ssttpcalculator.legacy.util

import config.AppConfig
import journey.Journey
import play.api.mvc.Request
import ssttpcalculator.CalculatorType.{PaymentOptimised, Legacy}
import ssttpcalculator.legacy.CalculatorService
import ssttpcalculator.model.PaymentSchedule
import ssttpcalculator.PaymentPlansService

trait CalculatorSwitchSelectedScheduleHelper {
  val calculatorService: CalculatorService
  val paymentPlansService: PaymentPlansService
  implicit val appConfig: AppConfig

  protected def selectedSchedule(journey: Journey)(implicit request: Request[_]): PaymentSchedule = {
    maybeSelectedSchedule(journey).getOrElse(
      throw new IllegalArgumentException("could not calculate a valid schedule but there should be one")
    )
  }

  protected def maybeSelectedSchedule(journey: Journey)(implicit request: Request[_]): Option[PaymentSchedule] = {
    appConfig.calculatorType match {
      case Legacy => Some(calculatorService.selectedSchedule(journey))
      case PaymentOptimised =>
        paymentPlansService.selectedSchedule(journey)
    }
  }

}
