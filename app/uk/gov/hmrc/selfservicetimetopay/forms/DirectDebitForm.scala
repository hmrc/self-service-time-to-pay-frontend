/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDirectDebit

object DirectDebitForm {
  val createDirectDebitForm = Form(mapping(
      "accountHolderName" -> nonEmptyText,
      "sortCode1" -> number(min = 0, max = 99),
      "sortCode2" -> number(min = 0, max = 99),
      "sortCode3" -> number(min = 0, max = 99),
      "accountNumber" -> longNumber(min = 0, max = 999999999),
      "confirmed" -> optional(boolean)
    )(ArrangementDirectDebit.apply)(ArrangementDirectDebit.unapply))
}
