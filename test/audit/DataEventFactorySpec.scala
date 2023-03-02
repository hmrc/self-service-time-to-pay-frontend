
package audit

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testsupport.UnitSpec
import testsupport.testdata.{TdAll, TdRequest}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

class DataEventFactorySpec extends UnitSpec {
  private val td = TdAll
  private val tdRequest = TdRequest
//  private val origin = Origin.SdilDdf
  private implicit val request: FakeRequest[AnyContentAsEmpty.type] = tdRequest.request

  private def splunkEventTags(transName: String) = Map(
    "clientIP" -> tdRequest.trueClientIp,
    "path" -> tdRequest.requestPath,
    "X-Session-ID" -> tdRequest.rawSessionId,
    "Akamai-Reputation" -> tdRequest.akamaiReputationValue,
    "X-Request-ID" -> tdRequest.requestId,
    "deviceID" -> tdRequest.deviceId,
    "clientPort" -> tdRequest.trueClientPort,
    "transactionName" -> transName
  )

  "Splunk audit events" - {

    s"manualAffordabilityCheck" in {
      val computedDataEvent = DataEventFactory.manualAffordabilityCheck()

      val expectedDataEvent = ExtendedDataEvent(
        auditSource = "pay-what-you-owe",
        auditType = "ManualAffordabilityCheck",
        eventId = "event-id",
        tags = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
        detail = Json.parse(
          s"""
            {
              "totalDebt": "5000",
              "spending": "9001.56",
              "income": "5000",
              "halfDisposalIncome": "-4001.56",
              "status": "Negative Disposable Income".
              "utr: "012324729"
            }
            """)
      )

      computedDataEvent.copy(eventId = "event-id", generatedAt = td.instant) shouldBe expectedDataEvent.copy(eventId = "event-id", generatedAt = td.instant)
    }

    s"manualAffordabilityPlanSetUp" in {
      val computedDataEvent = DataEventFactory.manualAffordabilityPlanSetUp()

      val expectedDataEvent = ExtendedDataEvent(
        auditSource = "pay-what-you-owe",
        auditType = "ManualAffordabilityPlanSetUp",
        eventId = "event-id",
        tags = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
        detail = Json.parse(
          s"""
          {
            "bankDetails": {
              "accountNumber": "86563611",
              "name": "Illumination",
              "sortCode": "207102",
            },
            "halfDisposalIncome: "41000.00",
            "selectionType: "fiftyPercent",
            "schedule": {
              totalPayable": "9001.56",
                   "installmentDate": "28",
                   "installments": [
                      {
                        "amount": "1500",
                        "installmentNumber": "1",
                        "paymentDate": "2023-08-28",
                      },
                      {
                        "amount": "1500",
                        "installmentNumber": 2",
                        "paymentDate": "2023-07-28",
                      },
                      {
                        "amount": "1500",
                         "installmentNumber": "3",
                         "paymentDate": "2023-06-28",
                      },
                      {
                        "amount": "1500",
                        "installmentNumber": "4",
                         "paymentDate": "2023-05-28"
                      },
                      {
                        "amount": "1500",
                        "installmentNumber": "5",
                         "paymentDate": "2023-04-28"
                      },
                      {
                        "amount": "1500",
                         "installmentNumber": "6",
                         "paymentDate": "2023-03-28"
                      }
                   ],
                   "initialPaymentAmount": "0",
                   "totalNoPayments": "6",
                   "totalInterestCharged": "1.56",
                   "totalPayable": "9001.56",
                   "totalPaymentWithoutInterest": "9000"
                },
                "status": "Success",
                "paymentReference": "paymentReference",
                "utr": "012324729"
             }
          }
          """)
      )
      computedDataEvent.copy(eventId = "event-id", generatedAt = td.instant) shouldBe expectedDataEvent.copy(eventId = "event-id", generatedAt = td.instant)
    }
  }
}
