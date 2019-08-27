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

package controllers.action

import javax.inject.Inject
import play.api.mvc._
import token.TTPSessionId
import token.TTPSessionId._

import scala.concurrent.{ExecutionContext, Future}

class CheckSessionAction @Inject() (
    cc: MessagesControllerComponents
) extends ActionFilter[MessagesRequest] {

  protected lazy val redirectToStartPage = Results.Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.start())
  //  /payment-plan-calculator
  protected lazy val redirectToPaymentPlanCalculator = Results.Redirect(ssttpcalculator.routes.CalculatorController.getPaymentPlanCalculator())

  def filter[A](request: MessagesRequest[A]): Future[Option[Result]] = {
    val response: Option[Result] = request.maybeTTPSessionId.fold[Option[Result]](
      if (request.uri.contains("payment-plan-calculator"))
        Some(redirectToPaymentPlanCalculator.withSession(request.session + token.TTPSessionId.newTTPSession()))
      else
        Some(redirectToStartPage.withSession(request.session + token.TTPSessionId.newTTPSession()))
    )(_ => None)

    Future.successful(response)
  }

  private implicit class GetTTPSessionOps[A](request: MessagesRequest[A]) {
    def maybeTTPSessionId: Option[TTPSessionId] = request.session.get(ttpSessionId).map(token.TTPSessionId.apply)
    def getTTPSessionId: TTPSessionId = maybeTTPSessionId.getOrElse(
      throw new RuntimeException(s"Expected $ttpSessionId to be in the play session")
    )
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}
