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

package model.enumsforforms

import enumeratum.Enum
import scala.collection.immutable

sealed trait IsSoleSignatory extends enumeratum.EnumEntry {
  def asBoolean: Boolean = this match {
    case IsSoleSignatory.Yes => true
    case IsSoleSignatory.No  => false
  }
}

object IsSoleSignatory extends Enum[IsSoleSignatory] {
  case object Yes extends IsSoleSignatory
  case object No extends IsSoleSignatory
  override def values: immutable.IndexedSeq[IsSoleSignatory] = findValues

  def booleanToIsSoleSignatory(isSoleSignatory: Boolean): IsSoleSignatory = if (isSoleSignatory) {
    IsSoleSignatory.Yes
  } else {
    IsSoleSignatory.No
  }
}
