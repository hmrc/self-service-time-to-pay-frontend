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

package testsupport

import java.time.LocalDate

trait DateSupport {
  def taxYearEnd(year: Int): LocalDate = LocalDate.of(year, april, _5th)

  // use the following to prevent noisy magic number warnings and perhaps improve readability
  val january = 1
  val february = 2
  val march = 3
  val april = 4
  val may = 5
  val june = 6
  val july = 7
  val august = 8
  val september = 9
  val october = 10
  val november = 11
  val december = 12

  val _1st = 1
  val _2nd = 2
  val _3rd = 3
  val _4th = 4
  val _5th = 5
  val _6th = 6
  val _7th = 7
  val _8th = 8
  val _9th = 9
  val _10th = 10
  val _11th = 11
  val _12th = 12
  val _13th = 13
  val _14th = 14
  val _15th = 15
  val _16th = 16
  val _17th = 17
  val _18th = 18
  val _19th = 19
  val _20th = 20
  val _21st = 21
  val _22nd = 22
  val _23rd = 23
  val _25th = 25
  val _27th = 27
  val _28th = 28
  val _29th = 29
  val _30th = 30
  val _31st = 31

  val _2016 = 2016
  val _2017 = 2017
  val _2018 = 2018
  val _2019 = 2019
  val _2020 = 2020
}
