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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateCalculatorStub {

  type HttpStatus = Int

  private val addWorkingsDaysUrlPath = "/date-calculator/add-working-days"

  def stubAddWorkingDays(result: Either[HttpStatus, JsValue]): StubMapping =
    stubFor(
      post(urlPathEqualTo(addWorkingsDaysUrlPath))
        .willReturn(
          result.fold(
            aResponse().withStatus(_),
            json => aResponse().withStatus(200).withBody(Json.prettyPrint(json))
          )
        )
    )

  def stubAddWorkingDays(result: LocalDate): StubMapping =
    stubAddWorkingDays(Right(
      Json.parse(s"""{"result": "${result.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}" }""")
    ))

  def verifyAddWorkingDaysCalled(date: LocalDate, numberOfWorkingDays: Int): Unit =
    verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo(addWorkingsDaysUrlPath))
        .withRequestBody(
          equalToJson(
            s"""
            |{
            |  "date": "${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
            |  "numberOfWorkingDaysToAdd": ${numberOfWorkingDays.toString},
            |  "regions": [ "EW" ]
            |}
            |""".stripMargin
          )
        )
    )

  def verifyAddWorkingDaysNotCalled(): Unit = verify(exactly(0), postRequestedFor(urlPathEqualTo(addWorkingsDaysUrlPath)))

}
