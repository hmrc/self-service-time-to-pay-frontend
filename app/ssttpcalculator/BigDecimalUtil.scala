/*
 * Copyright 2022 HM Revenue & Customs
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

package ssttpcalculator

object BigDecimalUtil {

  def roundDownToNearestTen(value: BigDecimal): BigDecimal = {
    BigDecimal((value.longValue() / 10) * 10)
  }

  def roundUpToNearestTen(value: BigDecimal): BigDecimal = {
    val roundedLong = (value.longValue() / 10) * 10
    if (roundedLong == value) value //when no rounding is needed (value is exact multiplication of 10)
    else BigDecimal(roundedLong + 10)
  }

}
