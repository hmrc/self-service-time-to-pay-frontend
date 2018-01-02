/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.connectors

import com.google.inject.{ImplementedBy, Singleton}
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.time.workingdays.{BankHoliday, BankHolidaySet}

import scala.concurrent.Future

@ImplementedBy(classOf[WSBankHolidaysConnectorImpl])
trait BankHolidaysConnector {
  protected implicit val bankHolidayReads: Reads[BankHoliday]       = Json.reads[BankHoliday]
  protected implicit val bankHolidaySetReads: Reads[BankHolidaySet] = Json.reads[BankHolidaySet]
  val url:String
  val http: HttpGet

  def bankHolidays(division: String = "england-and-wales")(implicit hc: HeaderCarrier): Future[BankHolidaySet] = {
    http.GET[Map[String, BankHolidaySet]](url) map {
      holidaySets => holidaySets(division)
    }
  }
}
@Singleton
class WSBankHolidaysConnectorImpl extends BankHolidaysConnector with ServicesConfig {
  override val http = WSHttp
  val url = getConfString("bank-holidays.url", "")
}
