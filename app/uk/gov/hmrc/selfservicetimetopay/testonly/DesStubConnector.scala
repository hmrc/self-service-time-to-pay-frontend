/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.testonly

import com.google.inject.Singleton
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


@Singleton
class DesStubConnector extends ServicesConfig {

  def setReturns(tu: TestUser)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val predefinedResponse: JsValue = Json.obj(
      "status" -> tu.returnsResponseStatusCode,
      "body" -> tu.returns
    )

    val setReturnsUrl = s"$baseUrl/sa/taxpayer/${tu.utr.v}/returns"

    WSHttp
      .PATCH(setReturnsUrl, predefinedResponse)
      .map{
        r =>
          if(r.status != 200) throw new RuntimeException(s"Could not set up taxpayer's return in DES-STUB: $tu")
          Logger.debug(s"Set up a predefined return in DES-STUB for $tu")
      }
  }

  def setDebits(tu: TestUser)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val predefinedResponse: JsValue = Json.obj(
      "status" -> tu.debitsResponseStatusCode,
      "body" -> tu.debits
    )

    val setReturnsUrl = s"$baseUrl/sa/taxpayer/${tu.utr.v}/debits"

    WSHttp
      .PATCH(setReturnsUrl, predefinedResponse)
      .map{
        r =>
          if(r.status != 200) throw new RuntimeException(s"Could not set up taxpayer's debit in DES-STUB: $tu")
          Logger.debug(s"Set up a predefined debit in DES-STUB for $tu")
      }
  }

  private lazy val baseUrl: String = baseUrl("des-services")

  private def nextTitle(): String = choose("Mr", "Miss", "Mrs", "Ms")

  private def nextForname(): String = choose(
    "Sophia","Emma","Olivia","Ava","Mia","Isabella","Riley","Aria","Zoe","Charlotte","Lily","Layla","Amelia","Emily","Madelyn","Aubrey","Adalyn","Madison","Chloe","Harper","Abigail","Aaliyah","Avery","Evelyn","Kaylee","Ella","Ellie","Scarlett","Arianna","Hailey","Nora","Addison","Brooklyn","Hannah","Mila","Leah","Elizabeth","Sarah","Eliana","Mackenzie","Peyton","Maria","Grace","Adeline","Elena","Anna","Victoria","Camilla","Lillian",
    "Jackson", "Aiden", "Lucas", "Liam", "Noah", "Ethan", "Mason", "Caden", "Oliver", "Elijah", "Grayson", "Jacob", "Michael", "Benjamin", "Carter", "James", "Jayden", "Logan", "Alexander", "Caleb", "Ryan", "Luke", "Daniel", "Jack", "William", "Owen", "Gabriel", "Matthew", "Connor", "Jayce", "Isaac", "Sebastian", "Henry", "Muhammad", "Cameron", "Wyatt", "Dylan", "Nathan", "Nicholas", "Julian", "Eli", "Levi", "Isaiah", "Landon", "David", "Christian", "Andrew", "Brayden", "John"
  )

  private def nextSurname(): String = choose(
    "Williams","Johnson","Taylor","Thomas","Roberts","Khan","Lewis","Jackson","Clarke","James","Phillips","Wilson","Ali","Mason","Mitchell","Rose","Davis","Davies","Rodriguez","Cox","Alexander","Morgan","Moore","Mills","King","Adams","Garcia","White","Stone","Edwards","Watson","Malley","Walker","Austin","Pearce","Reid","Simon"
  )

  private def choose(s0: String, ss: String*) = {
    val choices = s0 :: ss.toList
    val choice = Random.nextInt(choices.size)
    choices(choice)
  }

}
