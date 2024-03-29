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

import play.api

trait Logging {
  val inClass: Class[_] = getClass

  /**
   * Logger with tag "application". Uses implicit request to report request context.
   */
  val appLogger = logger("application")

  /**
   * Logger with tag "journey". Uses implicit request and journey to report request and journey context.
   */
  val journeyLogger = new JourneyLogger(inClass)

  /**
   * Logger with tag "connections". Uses implicit request to report request context.
   */
  val connectionsLogger = new ConnectionsLogger(inClass)

  /**
   * Logger with tag "audit". Uses implicit request to report request context.
   */
  val auditLogger = logger("audit")

  /**
   * Logger with tag "stubs connections". Uses implicit request to report request context.
   */
  val stubsConnectionsLogger = logger("stubs connections")

  /**
   * Logger with parameterized tag. Uses implicit request to report request context.
   * @param reference Logger name/tag to display
   */
  def logger(reference: String) = new Logger(reference, inClass)

  /**
   * Logger without context. Parameterized tag. Only use when not wanting to pass implicit request. If request is available use appLogger or other logger with context
   * @param reference Logger name/tag to display
   */
  def contextlessLogger(reference: String): api.Logger = play.api.Logger(reference)
}
