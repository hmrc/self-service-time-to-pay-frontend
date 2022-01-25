/*
 * Copyright 2022 HM Revenue & Customs
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

package bankholidays

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Clock, LocalDate}

class WorkingDaysServiceSpec extends AnyWordSpec with Matchers {

  implicit val c: Clock = Clock.systemUTC()

  "calculator next day" should {
    "for a workday" in {
      WorkingDaysService.addWorkingDays(LocalDate.of(2021, 8, 31), 1) shouldBe LocalDate.of(2021, 9, 1)
    }

    "for a weekend" in {
      WorkingDaysService.addWorkingDays(LocalDate.of(2021, 9, 3), 1) shouldBe LocalDate.of(2021, 9, 6)
    }

    "for Queen's Platinum Jubilee 2020 test" in {
      WorkingDaysService.addWorkingDays(LocalDate.of(2022, 6, 3), 1) shouldBe LocalDate.of(2022, 6, 6)
    }
  }

}
