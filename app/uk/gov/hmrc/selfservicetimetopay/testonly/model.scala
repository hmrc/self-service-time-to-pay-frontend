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

import scala.util.Random

case class TestUser(
  utr: Utr = Utr.random(),
  hasSAEnrolment: Boolean = true,
  authorityId: AuthorityId = AuthorityId.random,
  affinityGroup: AffinityGroup = AffinityGroup.Individual,
  confidenceLevel: Int = 200
)

case class AffinityGroup(v: String)


object AffinityGroup {
  val Individual = AffinityGroup("Individual")
}

/**
  * The same as CredId
  */
case class AuthorityId(v: String)

object AuthorityId {
  def random = AuthorityId(s"authorityId-${Math.abs(Random.nextLong())}")
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
