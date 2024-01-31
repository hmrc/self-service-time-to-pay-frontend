/*
 * Copyright 2023 HM Revenue & Customs
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

package ssttpeligibility

import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions
import enrolforsa.AddTaxesConnector

import javax.inject._
import play.api.mvc._
import req.RequestSupport
import util.Logging
import views.Views

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SelfServiceTimeToPayController @Inject() (
    mcc:             MessagesControllerComponents,
    as:              Actions,
    views:           Views,
    requestSupport:  RequestSupport,
    addTaxConnector: AddTaxesConnector)(implicit appConfig: AppConfig,
                                        ec: ExecutionContext
) extends FrontendBaseController(mcc) with Logging {

  import requestSupport._

  val start: Action[AnyContent] = as.action { implicit request =>
    Ok(views.service_start(isSignedIn, mcc.messagesApi))
  }

  val doStart: Action[AnyContent] = as.action { implicit request =>
    Redirect(ssttparrangement.routes.ArrangementController.determineEligibility)
  }

  private val getCallUsAboutAPaymentPlan: Action[AnyContent] = as.action { implicit request =>
    Ok(views.call_us_about_a_payment_plan(isWelsh, loggedIn = isSignedIn))
  }

  val getTtpCallUs: Action[AnyContent] = getCallUsAboutAPaymentPlan
  val getTtpCallUsTypeOfTax: Action[AnyContent] = getCallUsAboutAPaymentPlan
  val getTtpCallUsExistingTTP: Action[AnyContent] = getCallUsAboutAPaymentPlan
  val getTtpCallUsCalculatorInstalments: Action[AnyContent] = getCallUsAboutAPaymentPlan
  val getTtpCallUsSignInQuestion: Action[AnyContent] = getCallUsAboutAPaymentPlan
  val getNotSaEnrolled: Action[AnyContent] = getCallUsAboutAPaymentPlan

  val callUsCannotSetUpPlan: Action[AnyContent] = as.action { implicit request =>
    Ok(views.call_us_cannot_set_up_plan(isWelsh, loggedIn = isSignedIn))
  }

  val getDebtTooOld: Action[AnyContent] = as.action { implicit request =>
    Ok(views.call_us_debt_too_old(isWelsh, loggedIn = isSignedIn))
  }

  val getDebtTooLarge: Action[AnyContent] = as.action { implicit request =>
    Ok(views.debt_too_large(isSignedIn, isWelsh))
  }

  val getFileYourTaxReturn: Action[AnyContent] = as.action { implicit request =>
    Ok(views.file_your_tax_return(isSignedIn))
  }

  val getYouAlreadyHaveAPaymentPlan: Action[AnyContent] = as.action { implicit request =>
    Ok(views.you_already_have_a_payment_plan(isSignedIn, isWelsh))
  }

  val getAccessYouSelfAssessmentOnline: Action[AnyContent] = as.action { implicit request =>
    Ok(views.you_need_to_request_access_to_self_assessment(isWelsh, isSignedIn))
  }

  val getNotSoleSignatory: Action[AnyContent] = as.action { implicit request =>
    Ok(views.not_sole_signatory(isWelsh, isSignedIn))
  }

  val submitAccessYouSelfAssessmentOnline: Action[AnyContent] = as.authenticatedSaUser.async { implicit request =>
    val logMessagePrefix = "Submit 'Access your self assessment online': "
    val logMessage = logMessagePrefix + "Sending user to PTA (add-taxes-frontend) to enroll for SA" +
      s"[utr=${request.maybeUtr.map(_.obfuscate)}]"
    appLogger.info(logMessage)

    val resultF = request.credentials match {
      case Some(credentials) =>
        for {
          startIdentityVerificationJourneyResult <- addTaxConnector.startEnrolForSaJourney(request.maybeUtr, credentials)
          redirectUrl = startIdentityVerificationJourneyResult.redirectUrl
        } yield Redirect(redirectUrl)
      case None =>
        //Use kibana to monitor how often this happens. We were told that majority of users should have credentials.
        appLogger.warn(
          logMessagePrefix + "[Failed] Rotten credentials error. " +
            "The auth microservice returned empty credentials which are required to be passed " +
            "to add-taxes-frontend in order to enrol-for-sa. Please investigate. " +
            "Redirecting user to 'Not enrolled in SA' kick-out page"
        )
        Future.successful(Redirect(
          ssttpeligibility.routes.SelfServiceTimeToPayController.getNotSaEnrolled
        ))
    }

    resultF.recover {
      case NonFatal(ex) =>
        appLogger.warn(logMessagePrefix + s"[Failed] $logMessage", ex)
        throw ex
    }
  }

  val signOut: Action[AnyContent] = as.action { _ =>
    Redirect(appConfig.logoutUrl).withNewSession
  }
}
