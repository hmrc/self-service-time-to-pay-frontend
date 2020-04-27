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

import org.scalatest.{Matchers, WordSpec}
import ssttpdirectdebit.DirectDebitUtils.bankDetails
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, DirectDebitBank, DirectDebitInstruction}

class DirectDebitUtilsSpec extends WordSpec with Matchers {
  private val date = "processingDate"
  private val sortCode = "sortCode"
  private val accountNumber = "accountNumber"
  private val accountName = "accountName"

  private val userSpecifiedBankDetails = BankDetails(Some(sortCode), Some(accountNumber), accountName = Some(accountName))

  private val instructionForAnotherAccount = DirectDebitInstruction(accountNumber = Some("other account"), sortCode = Some(sortCode))
  private val instructionForAnotherSortCode = DirectDebitInstruction(accountNumber = Some(accountNumber), sortCode = Some("other sort code"))

  private val referenceNumber1 = Some("reference number 1")

  private val matchingDirectDebitInstruction =
    DirectDebitInstruction(accountNumber   = Some(accountNumber), sortCode = Some(sortCode), referenceNumber = referenceNumber1)

  private val expectedBankDetails =
    BankDetails(Some(sortCode), Some(accountNumber), accountName = Some(accountName), ddiRefNumber = referenceNumber1)

  "bankDetails" should {
    "return the user-specified bank details" when {
      "no direct debit instructions are provided" in {
        bankDetails(sortCode, accountNumber, accountName, DirectDebitBank(date, Seq.empty)) shouldBe userSpecifiedBankDetails
      }

      "no direct debit instructions are provided for the specified account number" in {
        bankDetails(
          sortCode, accountNumber, accountName, DirectDebitBank(date, Seq(instructionForAnotherAccount))) shouldBe userSpecifiedBankDetails
      }

      "no direct debit instructions are provided for the specified sort code" in {
        bankDetails(
          sortCode, accountNumber, accountName, DirectDebitBank(date, Seq(instructionForAnotherSortCode))) shouldBe userSpecifiedBankDetails
      }

      "a direct debit instruction is provided which matches the specified account number and sort code but has no reference number" in {
        val directDebit = DirectDebitBank(date, Seq(matchingDirectDebitInstruction.copy(referenceNumber = None)))

        bankDetails(sortCode, accountNumber, accountName, directDebit) shouldBe userSpecifiedBankDetails
      }
    }

    "return the matching bank details" when {
      "a single direct debit instruction matching the specified account number and sort code is provided" in {
        bankDetails(
          sortCode, accountNumber, accountName, DirectDebitBank(date, Seq(matchingDirectDebitInstruction))) shouldBe expectedBankDetails
      }

      "multiple direct debit instructions are provided including one that matches the specified account number and sort code" in {
        val directDebits =
          DirectDebitBank(date, Seq(matchingDirectDebitInstruction, instructionForAnotherAccount, instructionForAnotherSortCode))

        bankDetails(sortCode, accountNumber, accountName, directDebits) shouldBe expectedBankDetails
      }
    }

    "return the matching bank details with the minimum refernce number" when {
      "multiple direct debit instructions are provided which match the specified account number and sort code" in {
        val directDebits =
          DirectDebitBank(
            date, Seq(matchingDirectDebitInstruction, matchingDirectDebitInstruction.copy(referenceNumber = Some("reference number 2"))))

        bankDetails(sortCode, accountNumber, accountName, directDebits) shouldBe expectedBankDetails
      }
    }
  }
}
