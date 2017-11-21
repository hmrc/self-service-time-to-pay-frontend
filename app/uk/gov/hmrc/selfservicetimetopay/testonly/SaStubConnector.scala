/*
 * Copyright 2017 HM Revenue & Customs
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
class SaStubConnector extends ServicesConfig {

  def setTaxpayerResponse(utr: Utr)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val predefinedResponse: JsValue = Json.obj(
      "status" -> 200,
      "body" -> buildTaxpayer()
    )
    val setTaxpayerUrl = s"$baseUrl/sa/individual/${utr.v}/designatory-details/taxpayer"

    WSHttp
      .PATCH(setTaxpayerUrl, predefinedResponse)
      .map{
        r =>
          if(r.status != 200) throw new RuntimeException(s"Could not set up taxpayer in SA-STUB: $utr")
          Logger.debug(s"Set up a predefined response in SA-STUB for $utr")
      }
  }

  private lazy val baseUrl: String = baseUrl("sa-services")
  private def buildTaxpayer(): JsObject = {
    val title = nextTitle()
    val forname = nextForname()
    val surname = nextSurname()
    val email = s"$title.$forname.$surname@$surname$surname.com"
    Json.obj(
      "name" -> Json.obj(
        "title" -> title,
        "forename" -> forname,
        "surname" -> surname,
        "honours" -> "KCBE"
      ),
      "address" -> Json.obj(
        "addressLine1" -> "123 Any Street",
        "addressLine2" -> "Kingsland High Road",
        "addressLine3" -> "Dalston",
        "addressLine4" -> "Greater London",
        "addressLine5" -> "",
        "postcode" -> "E8 3PP",
        "additionalDeliveryInformation" -> "Watch the dog"
      ),
      "contact" -> Json.obj(
        "telephone" -> Json.obj(
          "daytime" -> "02765760#1235",
          "evening" -> "027657630",
          "mobile" -> "rowMapper",
          "fax" -> "0208875765"
        ),
        "email" -> Json.obj(
          "primary" -> email
        )
      )
    )
  }

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
