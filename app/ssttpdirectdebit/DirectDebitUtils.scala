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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, DirectDebitBank, DirectDebitInstruction}

object DirectDebitUtils {
  def bankDetails(sortCode: String, accountNumber: String, accountName: String, directDebitBank: DirectDebitBank)
    (implicit hc: HeaderCarrier): BankDetails = {
    val instructions = directDebitBank.directDebitInstruction.filter { p =>
      p.accountNumber.getOrElse("").equalsIgnoreCase(accountNumber) && p.sortCode.getOrElse("").equals(sortCode)
    }

    val referenceNumbersMatchingBankDetails = instructions.map(_.referenceNumber)

    JourneyLogger.info(s"DirectDebitUtils.bankDetails: referenceNumbersMatchingBankDetails [$referenceNumbersMatchingBankDetails]")

      def minReferenceNumber(a: DirectDebitInstruction, b: DirectDebitInstruction): DirectDebitInstruction =
        (a.referenceNumber, b.referenceNumber) match {
          case (Some(referenceNumberA), Some(referenceNumberB)) =>
            if (math.Ordering[String].lteq(referenceNumberA, referenceNumberB)) a else b
          case (None, Some(_)) => b
          case _               => a
        }

    val userSuppliedBankDetails = BankDetails(sortCode, accountNumber, accountName)

    val maybeFilteredDDI = instructions.reduceOption(minReferenceNumber)
    val maybeMinReferenceNumber = maybeFilteredDDI.fold(Option.empty[String])(_.referenceNumber)

    JourneyLogger.info(s"DirectDebitUtils.bankDetails: maybeMinReferenceNumber [${maybeMinReferenceNumber}]")

    maybeFilteredDDI.fold(userSuppliedBankDetails) {
      case instruction @ DirectDebitInstruction(Some(foundSortCode), Some(foundAccountNumber), _, _, _, _, _, _) =>
        BankDetails(
          foundSortCode,
          foundAccountNumber,
          accountName,
          maybeDDIRefNumber = instruction.referenceNumber
        )
      case _ => userSuppliedBankDetails
    }
  }
}
