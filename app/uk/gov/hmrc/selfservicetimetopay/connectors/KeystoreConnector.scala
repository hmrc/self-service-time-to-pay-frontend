package uk.gov.hmrc.selfservicetimetopay.connectors

import play.api.http.Status
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class KeystoreConnector(sessionCache : SessionCache) {

  def saveData[T](key: String, data: T)(implicit hc:HeaderCarrier, format: Format[T]) : Future[CacheMap] = {
    sessionCache.cache[T](key,data)
  }


  def fetch[T](key: String)(implicit hc:HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    sessionCache.fetchAndGetEntry(key)
  }

  def clear()(implicit hc:HeaderCarrier): Future[Boolean] = {
    sessionCache.remove().map( _.status == Status.NO_CONTENT)
  }

}
