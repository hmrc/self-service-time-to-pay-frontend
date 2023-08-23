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

import cats.implicits.catsSyntaxEq
import play.api.{Logger => PlayLogger}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import ssttparrangement.SubmissionError
import uk.gov.hmrc.http.CookieNames
import uk.gov.hmrc.selfservicetimetopay.models.{PaymentPlanRequest, TTPArrangement}
import util.RequestSupport.hc

class Logger(reference: String, inClass: Class[_]) extends BaseLogger(inClass) {
  val log: play.api.Logger = PlayLogger(reference)
}

abstract class BaseLogger(inClass: Class[_]) {

  val log: play.api.Logger

  def debug(message: => String)(implicit request: RequestHeader): Unit = logMessage(message, Debug)

  def info(message: => String)(implicit request: RequestHeader): Unit = logMessage(message, Info)

  def warn(message: => String)(implicit request: RequestHeader): Unit = logMessage(message, Warn)

  def error(message: => String)(implicit request: RequestHeader): Unit = logMessage(message, Error)

  def debug(message: => String, ex: Throwable)(implicit request: RequestHeader): Unit = logMessage(message, ex, Debug)

  def info(message: => String, ex: Throwable)(implicit request: RequestHeader): Unit = logMessage(message, ex, Info)

  def warn(message: => String, ex: Throwable)(implicit request: RequestHeader): Unit = logMessage(message, ex, Warn)

  def error(message: => String, ex: Throwable)(implicit request: RequestHeader): Unit = logMessage(message, ex, Error)

  protected def source = s"[class: $inClass]"

  protected def context(implicit rh: RequestHeader) = s"$request $sessionId $requestId $referer $deviceId"

  protected def request(implicit rh: RequestHeader) = s"[request: ${rh.method} ${rh.path}]"

  protected def sessionId(implicit rh: RequestHeader) = s"[sessionId: ${hc.sessionId.map(_.value).getOrElse("-")}]"

  protected def requestId(implicit rh: RequestHeader) = s"[requestId: ${hc.requestId.map(_.value).getOrElse("-")}]"

  protected def referer(implicit r: RequestHeader) = s"[referer: ${r.headers.headers.find(_._1 === "Referer").map(_._2).getOrElse("")}]"

  protected def deviceId(implicit r: RequestHeader) = s"[deviceId: ${r.cookies.find(_.name === CookieNames.deviceID).map(_.value).getOrElse("")}]"

  protected def makeRichMessage(message: String)(implicit request: RequestHeader): String = s"$message $source $context "

  protected def appendedData(data: JsValue): String = s"\n${Json.prettyPrint(data)}"

  protected sealed trait LogLevel

  protected case object Debug extends LogLevel

  protected case object Info extends LogLevel

  protected case object Warn extends LogLevel

  protected case object Error extends LogLevel

  protected def logMessage(message: => String, level: LogLevel)(implicit request: RequestHeader): Unit = {
    lazy val richMessage = makeRichMessage(message)
    level match {
      case Debug => log.debug(richMessage)
      case Info  => log.info(richMessage)
      case Warn  => log.warn(richMessage)
      case Error => log.error(richMessage)
    }
  }

  protected def logMessage(message: => String, ex: Throwable, level: LogLevel)(implicit request: RequestHeader): Unit = {
    lazy val richMessage = makeRichMessage(message)
    level match {
      case Debug => log.debug(richMessage, ex)
      case Info  => log.info(richMessage, ex)
      case Warn  => log.warn(richMessage, ex)
      case Error => log.error(richMessage, ex)
    }
  }

}
