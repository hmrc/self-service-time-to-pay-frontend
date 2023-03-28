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

package util

object CurrencyUtil {

  /*
  The regex will match currency input with a complex format:
  - Spaces anywhere preceding or following any of the input characters
  - One optional £ symbol preceding any digits, but only if it is followed by at least one digit
  - One optional hyphen before the £ symbol or one optional hyphen after the £ symbol, but only one of them at one time. Both will be considered valid negative inputs.
  - Optional commas as thousands separators, but only in valid positions.
  - Optional dot as decimal separator followed by unlimited digits.

  e.g. will match:
  123, 45 6
  £ 1, 2 34,5 67.8 99
    -   £ 10
  £ - 10
  -1 0
  £.1

  e.g will not match:
  - £-10
  - - 10
  12,34.5
  £
  £ .
   */

  def regex: String = "(?!.*-.*-)[ ]*(-)?[ ]*(\\£)?[ ]*(?=.*\\d)(-)?[ ]*((?:\\d\\s*){1,3}(?:[ ]*)(\\,(?:[ ]*)(?:\\d\\s*){3})*|(\\d+))?((?:[ ]*)\\.[ ]*\\d+(?:[ ]*\\d+)*[ ]*)?$"

  def cleanAmount(text: String): String = text
    .replace("£", "")
    .replaceAll(" ", "")
    .replaceAll(",", "")
    .trim

}