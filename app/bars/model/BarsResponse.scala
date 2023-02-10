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

package bars.model

import play.api.libs.json.{Json, OFormat}
import julienrf.json.derived

sealed trait BarsResponse

object BarsResponse {
  implicit val format: OFormat[BarsResponse] = derived.oformat()
}

final case class BarsResponseOk(validateBankDetailsResponse: ValidateBankDetailsResponse) extends BarsResponse

object BarsResponseOk {
  implicit val format: OFormat[BarsResponseOk] = Json.format[BarsResponseOk]

}

final case class BarsResponseSortCodeOnDenyList(barsError: BarsError) extends BarsResponse

object BarsResponseSortCodeOnDenyList {
  implicit val format: OFormat[BarsResponseSortCodeOnDenyList] = Json.format[BarsResponseSortCodeOnDenyList]
}
