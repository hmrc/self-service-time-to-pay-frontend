/*
 * Copyright 2020 HM Revenue & Customs
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

package ssttpdirectdebit

import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, DirectDebitBank, DirectDebitInstruction}

object DirectDebitUtils {
  def bankDetails(sortCode: String, accountNumber: String, accountName: String, directDebitBank: DirectDebitBank): BankDetails = {
    val instructions = directDebitBank.directDebitInstruction.filter { p =>
      p.accountNumber.fold(false)(_.equalsIgnoreCase(accountNumber)) && p.sortCode.fold(false)(_.equals(sortCode))
    }

      def minReferenceNumber(a: DirectDebitInstruction, b: DirectDebitInstruction): DirectDebitInstruction =
        (a.referenceNumber, b.referenceNumber) match {
          case (Some(referenceNumberA), Some(referenceNumberB)) =>
            if (math.Ordering[String].lteq(referenceNumberA, referenceNumberB)) a else b
          case (None, Some(_)) => b
          case _               => a
        }

    instructions
      .reduceOption(minReferenceNumber)
      .fold(BankDetails(Some(sortCode), Some(accountNumber), accountName = Some(accountName))) { instruction =>
        BankDetails(
          instruction.sortCode,
          instruction.accountNumber,
          accountName  = Some(accountName),
          ddiRefNumber = instruction.referenceNumber
        )
      }
  }
}
