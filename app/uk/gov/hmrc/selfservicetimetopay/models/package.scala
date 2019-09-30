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

package uk.gov.hmrc.selfservicetimetopay

import play.api.libs.json._
import uk.gov.hmrc.selfservicetimetopay.models._

package object modelsFormat {

  implicit def eitherReads[A, B](implicit A: Reads[A], B: Reads[B]): Reads[Either[A, B]] =
    Reads[Either[A, B]] { json =>
      A.reads(json) match {
        case JsSuccess(value, path) => JsSuccess(Left(value), path)
        case JsError(e1) => B.reads(json) match {
          case JsSuccess(value, path) => JsSuccess(Right(value), path)
          case JsError(e2)            => JsError(JsError.merge(e1, e2))
        }
      }
    }

  implicit def eitherWrites[A, B](implicit A: Writes[A], B: Writes[B]): Writes[Either[A, B]] =
    Writes[Either[A, B]] {
      case Left(obj)  => A.writes(obj)
      case Right(obj) => B.writes(obj)
    }

  //Front end formatters

  implicit val bankAccountResponseFormatter = Format(eitherReads[BankDetails, DirectDebitBank], eitherWrites[BankDetails, DirectDebitBank])

  //Eligibility formatters
  //TODO make sure that function returns the correct Reason when case is ttpislessthentwomonths
  def parseFromString(jsonString: String): Option[Reason] = jsonString.trim.toLowerCase match {
    case "nodebt"                                 => Some(NoDebt)
    case "debtisinsignificant"                    => Some(DebtIsInsignificant)
    case "olddebtistoohigh"                       => Some(OldDebtIsTooHigh)
    case "totaldebtistoohigh"                     => Some(TotalDebtIsTooHigh)
    case "ttpislessthentwomonths"                 => Some(TotalDebtIsTooHigh)
    case "isnotonia"                              => Some(IsNotOnIa)
    case x if x.contains("returnneedssubmitting") => Some(ReturnNeedsSubmitting)
    case _ =>
      None
  }

}
