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

package filters

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilter

import scala.concurrent.Future

/**
 * Special case for SessionTimeoutFilter which doesn't apply for one test-only endpoint.
 */
class SessionTimeoutFilterWrapper(sessionTimeoutFilter: SessionTimeoutFilter) extends Filter {
  override implicit def mat: Materializer = sessionTimeoutFilter.mat
  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (isSpecialRequest(rh)) f(rh) else sessionTimeoutFilter(f)(rh)

  private def isSpecialRequest(rh: RequestHeader) =
    rh.method == testonly.routes.TestUsersController.logIn().method &&
      rh.path == testonly.routes.TestUsersController.logIn().path()
}
