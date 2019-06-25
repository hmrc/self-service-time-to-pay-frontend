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

package testonly

import play.api.http.Status
import play.api.libs.json.JsValue

import scala.util.Random

case class TestUser(
    utr:                          Utr,
    hasSAEnrolment:               Boolean,
    isOnIA:                       Boolean,
    authorityId:                  AuthorityId,
    affinityGroup:                AffinityGroup  = AffinityGroup.individual,
    confidenceLevel:              Int,
    returns:                      JsValue,
    returnsResponseStatusCode:    Int,
    debits:                       JsValue,
    debitsResponseStatusCode:     Int,
    saTaxpayer:                   JsValue,
    saTaxpayerResponseStatusCode: Int,
    continueUrl:                  Option[String]
)

object TestUser {

  def exemplary() = TestUser(
    utr                          = Utr.random(),
    hasSAEnrolment               = true,
    isOnIA                       = true,
    authorityId                  = AuthorityId.random,
    affinityGroup                = AffinityGroup.individual,
    confidenceLevel              = 200,
    returns                      = TestUserReturns.sample1,
    returnsResponseStatusCode    = Status.OK,
    debits                       = TestUserDebits.sample1,
    debitsResponseStatusCode     = Status.OK,
    saTaxpayer                   = TestUserSaTaxpayer.buildTaxpayer(),
    saTaxpayerResponseStatusCode = Status.OK,
    continueUrl                  = None
  )

}

case class AffinityGroup(v: String)

object AffinityGroup {
  val individual = AffinityGroup("Individual")
  val organisation = AffinityGroup("Organisation")
  val agent = AffinityGroup("Agent")
}

/**
 * The same as CredId
 */
case class AuthorityId(v: String)

object AuthorityId {
  def random = AuthorityId(s"authId-${Math.abs(Random.nextLong() % 1000)}") //length must be < 25 characters (dunno why...)
}

case class AuthorityUri(v: String)

case class GatewayToken(v: String)

/**
 * The same as bearer token
 */
case class AuthToken(v: String)

case class Utr(v: String)

object Utr {
  def random(): Utr = Utr("1234512345".map(_ => nextDigitChar()))
  private def nextDigitChar() = Random.nextInt(10).toString.charAt(0)
}
