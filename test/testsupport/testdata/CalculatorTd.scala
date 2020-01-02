/*
 * Copyright 2020 HM Revenue & Customs
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

package testsupport.testdata

import java.time.LocalDate

import uk.gov.hmrc.selfservicetimetopay.models._
import testsupport.testdata.TdAll._
import testsupport.JsonSyntax._
import timetopaycalculator.cor.model.{CalculatorInput, Instalment, PaymentSchedule}

object CalculatorTd {
  val calculatorAmountsDue = new CalculatorAmountsDue(Seq(debit1, debit2))

  val calculatorAmountsDueJson =
    s"""{
      "amountsDue": [${debit1Json},${debit2Json}]
    }""".asJson

  val calculatorDuration = new CalculatorDuration(4)

  val calculatorDurationJson =
    //language=Json
    """{
    "chosenMonths": 4
    }""".asJson

  val calculatorPaymentScheduleInstalment = new Instalment("2019-08-25", 300: BigDecimal, 2: BigDecimal)

  val calculatorPaymentScheduleInstalmentJson =
    //language=Json
    """{
      "paymentDate": "2019-08-25",
      "amount": 300,
      "interest": 2
    }""".asJson

  val calculatorPaymentScheduleInstalment2 = new Instalment("2019-08-25", 120: BigDecimal, 4: BigDecimal)

  val calculatorPaymentScheduleInstalmentJson2 =
    //language=Json
    """{
      "paymentDate": "2019-08-25",
      "amount": 120,
      "interest": 4
    }""".asJson

  val calculatorPaymentScheduleInstalment3 = new Instalment("2019-08-25", 250: BigDecimal, 3: BigDecimal)

  val calculatorPaymentScheduleInstalmentJson3 =
    //language=Json
    """{
      "paymentDate": "2019-08-25",
      "amount": 250,
      "interest": 3
    }""".asJson

  val calculatorPaymentSchedule = new PaymentSchedule(
    startDate            = LocalDate.of(2019: Int, 4: Int, 23: Int),
    endDate              = LocalDate.of(2019: Int, 8: Int, 21: Int),
    initialPayment       = 123: BigDecimal,
    amountToPay          = 2500: BigDecimal,
    instalmentBalance    = 200: BigDecimal,
    totalInterestCharged = 200: BigDecimal,
    totalPayable         = 200: BigDecimal,
    instalments          = List(calculatorPaymentScheduleInstalment, calculatorPaymentScheduleInstalment2, calculatorPaymentScheduleInstalment3))

  val calculatorPaymentScheduleJson =
    s"""{
    "startDate" : "2019-04-23",
    "endDate" : "2019-08-21",
    "initialPayment" : 0,
     "amountToPay" : 4000,
     "instalmentBalance" : 200,
     "totalInterestCharged" : 200,
     "totalPayable" : 200,
     "instalments" : [${calculatorPaymentScheduleInstalmentJson}, ${calculatorPaymentScheduleInstalmentJson2}, ${calculatorPaymentScheduleInstalmentJson3}]
    }""".asJson

  val calculatorInput = new CalculatorInput(
    debits           = Seq.empty,
    initialPayment   = 200: BigDecimal,
    startDate        = "2017-02-15",
    endDate          = "2018-01-31",
    firstPaymentDate = None)

  val calculatorInputJson =
    //language=Json
    """{
       "debits": [],
    "initialPayment" : 200,
     "startDate": "2017-02-15",
     "endDate": "2018-01-31"
    }""".asJson
}
