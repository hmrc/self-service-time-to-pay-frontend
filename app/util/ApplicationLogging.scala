
package util

import play.api.Logger

/***
 * This logger is useful for repos which are not properly packaged under uk.gov.hmrc... etc
 * It allows easier logback configuration using the 'application' logger, eg:
 *
 *  <logger name="application" level="INFO"/>
 *
 *  <root level="WARN">
 *      <appender-ref ref="FILE"/>
 *      <appender-ref ref="STDOUT"/>
 *  </root>
 */
trait ApplicationLogging {
  val logger: Logger = Logger(s"application.${this.getClass.getName}")
}
