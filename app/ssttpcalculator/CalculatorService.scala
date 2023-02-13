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

package ssttpcalculator

import _root_.model._
import bankholidays.WorkingDaysService.addWorkingDays
import config.AppConfig
import journey.{Journey, PaymentToday}
import play.api.Logger
import play.api.mvc.Request
import ssttpcalculator.model.TaxLiability.{amortizedLiabilities, latePayments}
import ssttpcalculator.model.{Debit, FixedInterestPeriod, Instalment, InterestRate, LatePayment, LatePaymentInterest, Payable, Payables, Payment, PaymentSchedule, PaymentsCalendar, TaxLiability, TaxPaymentPlan}
import times.ClockProvider
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.LocalDate.now
import java.time.temporal.ChronoUnit.DAYS
import java.time.{Clock, LocalDate, Year}
import javax.inject.Inject
import scala.annotation.tailrec
import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode.HALF_UP

class CalculatorService @Inject() (
    clockProvider:       ClockProvider,
    durationService:     DurationService,
    interestService:     InterestRateService,
    paymentDatesService: PaymentDatesService
)
  (implicit ec: ExecutionContext,
   appConfig: AppConfig) {

  import clockProvider._

  val logger = Logger(getClass)

  val minimumMonthsAllowedTTP: Int = 2
  val DebitDueAndCalculationDatesWithinRate: (Boolean, Boolean) = Tuple2(true, true)
  val DebitDueDateWithinRate: (Boolean, Boolean) = Tuple2(true, false)
  val CalculationDateWithinRate: (Boolean, Boolean) = Tuple2(false, true)
  val defaultInitialPaymentDays: Int = 11
  val `14 day gap between the initial payment date and the first scheduled payment date`: Int = 14
  val LastPaymentDelayDays = 7

  val proportionsOfNetMonthlyIncome = List(0.5, 0.6, 0.8)

  // TODO [OPS-9610] currently will always create three plans (and hit issues with more than 24 months.
  // should only create plans for 60 and 80% if 50% is not one month long
  // needs to handle not creating a plan if it's more than 24 months long
  // proportions of netMonthlyIncome should be configurable
  def paymentPlanOptions(
      sa:                           SelfAssessmentDetails,
      initialPayment:               BigDecimal                    = BigDecimal(0),
      maybeArrangementDayOfMonth:   Option[ArrangementDayOfMonth],
      remainingIncomeAfterSpending: BigDecimal,
      maybePaymentToday:            Option[PaymentToday],
      dateToday:                    LocalDate
  )(implicit config: AppConfig): List[PaymentSchedule] = {

    val paymentsCalendar = paymentDatesService.paymentsCalendar(
      maybePaymentToday,
      maybeArrangementDayOfMonth,
      dateToday
    )(appConfig)

    val payables = Payables(
      liabilities = for {
        selfAssessmentDebit <- sa.debits
      } yield TaxLiability(selfAssessmentDebit.amount, selfAssessmentDebit.dueDate)
    )
    val firstSchedule = {
      val taxPaymentPlan: TaxPaymentPlan = TaxPaymentPlan(
        liabilities = payables.liabilities.map(Payable.payableToTaxLiability),
        initialPayment = initialPayment,
        startDate = paymentsCalendar.planStartDate,
        endDate = LocalDate.parse("2060-03-11"),
        firstPaymentDate = Some(paymentsCalendar.regularPaymentDates.headOption
          .getOrElse(throw new IllegalArgumentException("could not find first regular payment date, but there should be one"))
        ),
        maybeArrangementDayOfMonth = maybeArrangementDayOfMonth,
        regularPaymentAmount = proportionsOfNetMonthlyIncome(0) * remainingIncomeAfterSpending,
        maybePaymentToday)
      buildScheduleNew(taxPaymentPlan)
    }
    firstSchedule match {
      case Some(schedule) if (schedule.instalments.length <= 1) => List(firstSchedule).flatten
      case _ =>
        val secondSchedule = {
          val taxPaymentPlan: TaxPaymentPlan = TaxPaymentPlan(
            liabilities = payables.liabilities.map(Payable.payableToTaxLiability),
            initialPayment = initialPayment,
            startDate = paymentsCalendar.planStartDate,
            endDate = LocalDate.parse("2060-03-11"),
            firstPaymentDate = Some(paymentsCalendar.regularPaymentDates.headOption
              .getOrElse(throw new IllegalArgumentException("could not find first regular payment date, but there should be one"))
            ),
            maybeArrangementDayOfMonth = maybeArrangementDayOfMonth,
            regularPaymentAmount = proportionsOfNetMonthlyIncome(1) * remainingIncomeAfterSpending,
            maybePaymentToday)
          buildScheduleNew(taxPaymentPlan)
        }
        secondSchedule match {
          case Some(schedule) if (schedule.instalments.length <= 1) => List(firstSchedule, secondSchedule).flatten
          case _ =>
            val thirdSchedule = {
              val taxPaymentPlan: TaxPaymentPlan = TaxPaymentPlan(
                liabilities = payables.liabilities.map(Payable.payableToTaxLiability),
                initialPayment = initialPayment,
                startDate = paymentsCalendar.planStartDate,
                endDate = LocalDate.parse("2060-03-11"),
                firstPaymentDate = Some(paymentsCalendar.regularPaymentDates.headOption
                  .getOrElse(throw new IllegalArgumentException("could not find first regular payment date, but there should be one"))
                ),
                maybeArrangementDayOfMonth = maybeArrangementDayOfMonth,
                regularPaymentAmount = proportionsOfNetMonthlyIncome(2) * remainingIncomeAfterSpending,
                maybePaymentToday)
              buildScheduleNew(taxPaymentPlan)
            }
            List(firstSchedule, secondSchedule, thirdSchedule).flatten
        }
    }
  }

  def computeScheduleNew(journey: Journey)(implicit request: Request[_]): Option[PaymentSchedule] = {
    val paymentsCalendar = paymentDatesService.paymentsCalendar(
      journey.maybePaymentToday,
      journey.maybeArrangementDayOfMonth,
      clockProvider.nowDate()
    )(appConfig)

    val payables = Payables(
      liabilities = for {
        selfAssessmentDebit <- journey.taxpayer.selfAssessment.debits
      } yield TaxLiability(selfAssessmentDebit.amount, selfAssessmentDebit.dueDate)
    )

    val taxPaymentPlan: TaxPaymentPlan = TaxPaymentPlan(
      payables.liabilities.map(Payable.payableToTaxLiability),
      journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0)),
      paymentsCalendar.planStartDate,
      LocalDate.parse("2060-03-11"),
      Some(paymentsCalendar.regularPaymentDates.headOption
        .getOrElse(throw new IllegalArgumentException("could not find first regular payment date, but there should be one"))
      ), journey.maybeArrangementDayOfMonth,
      journey.maybePlanAmountSelection.getOrElse(throw new IllegalArgumentException("could not find plan amount selection but there should be one")),
      journey.maybePaymentToday
    )

    buildScheduleNew(taxPaymentPlan)
  }

  def buildScheduleNew(implicit taxPaymentPlan: TaxPaymentPlan): Option[PaymentSchedule] = {
    val payables: Payables = Payables(taxPaymentPlan.liabilities)
    val principal = payables.liabilities.map(_.amount).sum

    logger.info(s"payables: $payables")
    logger.info(s"principal: $principal")

    val paymentsCalendar = paymentDatesService.paymentsCalendar(
      maybePaymentToday          = taxPaymentPlan.maybePaymentToday,
      maybeArrangementDayOfMonth = taxPaymentPlan.firstPaymentDate.map(date => ArrangementDayOfMonth(date.getDayOfMonth)),
      dateToday                  = taxPaymentPlan.startDate
    )

    logger.info(s"payments:Calendar: $paymentsCalendar")

    val maybeInterestAccruedUpToStartDate: Option[LatePaymentInterest] = {
      totalHistoricInterest(payables, paymentsCalendar, interestService.getRatesForPeriod)
    }

    logger.info(s"maybeInterestAcccruedToStartDate: $maybeInterestAccruedUpToStartDate")

    val hasAnInitialPayment: Boolean = taxPaymentPlan.initialPayment > 0
    val upfrontPaymentLateInterest: BigDecimal = if (hasAnInitialPayment) {
      val initialPaymentDate: LocalDate = taxPaymentPlan.startDate.plusDays(defaultInitialPaymentDays)
      val debitsDueBeforeInitialPayment: Seq[TaxLiability] = taxPaymentPlan.liabilities.filter(_.dueDate.isBefore(initialPaymentDate))
      calculateInitialPaymentInterest(debitsDueBeforeInitialPayment)
    } else {
      BigDecimal(0)
    }

    logger.info(s"hasAnInitialPayment: $hasAnInitialPayment")
    logger.info(s"upfrontPaymentLateInterest: $upfrontPaymentLateInterest")

    val maybeUpfrontPaymentLateInterest: Option[LatePaymentInterest] = paymentsCalendar.maybeUpfrontPaymentDate match {
      case Some(_) => Option(LatePaymentInterest(upfrontPaymentLateInterest))
      case None    => None
    }

    logger.info(s"maybeUpFrontPaymentLateInterest: $maybeUpfrontPaymentLateInterest")

      def payablesResetLessUpfrontPayment(
          initialPayment: BigDecimal,
          liabilities:    Seq[TaxLiability],
          startDate:      LocalDate
      ): Seq[Payable] = {
        val result = liabilities.sortBy(_.dueDate).foldLeft((initialPayment, Seq.empty[TaxLiability])) {
          case ((p, s), lt) if p <= 0         => (p, s :+ lt.copy(dueDate = if (startDate.isBefore(lt.dueDate)) lt.dueDate else startDate))
          case ((p, s), lt) if p >= lt.amount => (p - lt.amount, s)
          case ((p, s), lt) => {
            (0, s :+ lt.copy(amount  = lt.amount - p, dueDate = {
              if (startDate.plusWeeks(1).isBefore(lt.dueDate)) lt.dueDate else {
                startDate.plusWeeks(1)
              }
            }))
          }
        }
        result._2
      }

    val resultPayablesResetLessUpfrontAlternative = payablesResetLessUpfrontPayment(
      taxPaymentPlan.initialPayment,
      payables.liabilities.map(Payable.payableToTaxLiability),
      paymentsCalendar.planStartDate
    )

    logger.info(s"resultPayablesResetLessUpfrontAlternative: $resultPayablesResetLessUpfrontAlternative")

    val liabilitiesUpdated: Seq[Payable] = Seq[Option[Payable]](maybeInterestAccruedUpToStartDate, maybeUpfrontPaymentLateInterest)
      .foldLeft(resultPayablesResetLessUpfrontAlternative)((ls, maybeInterest) => maybeInterest match {
        case Some(interest) => ls :+ interest
        case None           => ls
      })

    logger.info(s"liabilitiesUpdated: $liabilitiesUpdated")

    val payablesUpdated = Payables(liabilitiesUpdated)

    logger.info(s"payablesUpdated: $payablesUpdated")

    logger.info(s"tax payment plan: $taxPaymentPlan")

    val instalments = regularInstalments(paymentsCalendar, taxPaymentPlan.regularPaymentAmount, payablesUpdated, interestService.rateOn)

    logger.info(s"instalments: $instalments")

    instalments match {
      case None => None
      case Some(instalments) =>
        val instalmentLatePaymentInterest = instalments.map(_.interest).sum
        val totalInterestCharged = maybeInterestAccruedUpToStartDate.map(_.amount).getOrElse(BigDecimal(0)) +
          maybeUpfrontPaymentLateInterest.map(_.amount).getOrElse(0) + instalmentLatePaymentInterest
        Some(PaymentSchedule(
          startDate            = paymentsCalendar.planStartDate,
          endDate              = instalments
            .lastOption.map(_.paymentDate.plusDays(appConfig.lastPaymentDelayDays))
            .getOrElse(throw new IllegalArgumentException("no instalments found to create the schedule")),
          initialPayment       = taxPaymentPlan.initialPayment,
          amountToPay          = principal,
          instalmentBalance    = (principal - taxPaymentPlan.initialPayment),
          totalInterestCharged = totalInterestCharged,
          totalPayable         = (principal + totalInterestCharged),
          instalments          = instalments
        ))
    }
  }

  private def totalHistoricInterest(
      payables:         Payables,
      paymentsCalendar: PaymentsCalendar,
      periodToRates:    (LocalDate, LocalDate) => Seq[InterestRate]
  ): Option[LatePaymentInterest] = {
    val historicInterest = for {
      debt <- payables.liabilities.filter(_.hasInterestCharge(paymentsCalendar.planStartDate))
      debtFixedInterestPeriods = historicalFixedInterestPeriods(debt, periodToRates)(paymentsCalendar)

    } yield calculateHistoricInterest(debtFixedInterestPeriods)(paymentsCalendar)

    val totalHistoricInterest = historicInterest.sum
    if (totalHistoricInterest <= 0) {
      None
    } else {
      Some(LatePaymentInterest(amount = historicInterest.sum))
    }
  }

  private def historicalFixedInterestPeriods(
      payable:       Payable,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  )(implicit paymentsCalendar: PaymentsCalendar): Seq[FixedInterestPeriod] = {
    payable match {
      case LatePaymentInterest(_) => Seq()
      case TaxLiability(amount, dueDate) => {
        periodToRates(dueDate, paymentsCalendar.planStartDate)
          .map { rate =>
            (rate.containsDate(dueDate), rate.containsDate(paymentsCalendar.planStartDate)) match {
              case DebitDueAndCalculationDatesWithinRate => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = dueDate,
                endDate                = paymentsCalendar.planStartDate,
                rate                   = rate
              )
              case DebitDueDateWithinRate => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = dueDate,
                endDate                = rate.endDate,
                rate                   = rate
              )
              case CalculationDateWithinRate => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = rate.startDate,
                endDate                = paymentsCalendar.planStartDate,
                rate                   = rate
              )
              case _ => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = rate.startDate,
                endDate                = rate.endDate,
                rate                   = rate
              )
            }
          }
      }
    }

  }

  private def payablesDueDatesReset(payables: Payables, planStartDate: LocalDate): Payables = {
    payables.copy(liabilities = payables.liabilities.map(liability => liability.updateDueDate(planStartDate)))
  }

  private def latePaymentInterestFromUpfrontPayment(
      paymentsCalendar:     PaymentsCalendar,
      upfrontPaymentAmount: BigDecimal,
      payables:             Payables,
      periodToRates:        (LocalDate, LocalDate) => Seq[InterestRate]
  ): Option[LatePaymentInterest] = {
    val upfrontPaymentDate = paymentsCalendar.maybeUpfrontPaymentDate.getOrElse(
      throw new IllegalArgumentException("Calling to calculate late payment interest from upfront payment," +
        " but no upfront payment date found in payments calendar"
      )
    )

    if (payables.inDate(upfrontPaymentDate)) {
      None
    } else {
      val latePaymentInterestPortions = for {
        latePayment <- Payables.latePayments(upfrontPaymentAmount, upfrontPaymentDate, payables)
        latePaymentFixedInterestPeriods = fixedInterestPeriodFromLatePayment(latePayment, periodToRates)(paymentsCalendar)
      } yield calculateHistoricInterest(latePaymentFixedInterestPeriods)(paymentsCalendar)

      Some(LatePaymentInterest(amount = latePaymentInterestPortions.sum))
    }
  }

  // TODO OPS 9610: not necessary as only one interest on upfront payment, reimplement to use today's interest rate
  private def fixedInterestPeriodFromLatePayment(
      latePayment:   LatePayment,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  )(implicit paymentsCalendar: PaymentsCalendar): Seq[FixedInterestPeriod] = {
    periodToRates(latePayment.dueDate, paymentsCalendar.planStartDate)
      .map { rate =>
        (rate.containsDate(latePayment.dueDate), rate.containsDate(paymentsCalendar.planStartDate)) match {
          case DebitDueAndCalculationDatesWithinRate => FixedInterestPeriod(
            amountAccruingInterest = latePayment.amount,
            dueDate                = latePayment.dueDate,
            endDate                = paymentsCalendar.planStartDate,
            rate                   = rate
          )
          case DebitDueDateWithinRate => FixedInterestPeriod(
            amountAccruingInterest = latePayment.amount,
            dueDate                = latePayment.dueDate,
            endDate                = rate.endDate,
            rate                   = rate
          )
          case CalculationDateWithinRate => FixedInterestPeriod(
            amountAccruingInterest = latePayment.amount,
            dueDate                = rate.startDate,
            endDate                = paymentsCalendar.planStartDate,
            rate                   = rate
          )
          case _ => FixedInterestPeriod(
            amountAccruingInterest = latePayment.amount,
            dueDate                = rate.startDate,
            endDate                = rate.endDate,
            rate                   = rate
          )
        }
      }
  }

  private def payablesLessUpfrontPayment(upfrontPaymentAmount: BigDecimal, payables: Payables): Payables = {
    payablesUpdated(upfrontPaymentAmount, payables, None)
  }

  private def regularInstalments(paymentsCalendar:     PaymentsCalendar,
                                 regularPaymentAmount: BigDecimal,
                                 payables:             Payables,
                                 dateToRate:           LocalDate => InterestRate
  ): Option[Seq[Instalment]] = {
    regularInstalmentsRecursive(
      paymentsCalendar.planStartDate,
      paymentsCalendar.regularPaymentDates,
      regularPaymentAmount,
      payables,
      dateToRate,
      Seq.empty[Instalment]
    )
  }

  @tailrec
  private def regularInstalmentsRecursive(
      planStartDate:         LocalDate,
      regularPaymentDates:   Seq[LocalDate],
      maxPaymentAmount:      BigDecimal,
      payables:              Payables,
      dateToRate:            LocalDate => InterestRate,
      instalmentsAggregator: Seq[Instalment]
  ): Option[Seq[Instalment]] = {
    val paymentDate = regularPaymentDates.headOption
    val balance = payables.balance

    if (balance <= 0) {
      Some(instalmentsAggregator)
    } else {
      paymentDate match {
        case None => None
        case Some(nextPaymentDate) =>
          val maybeLatePaymentInterest = latePaymentInterest(maxPaymentAmount, nextPaymentDate, payables, dateToRate, planStartDate)
          val latePaymentInterestAmount: BigDecimal = maybeLatePaymentInterest.map(_.amount).getOrElse(0)

          val instalment = Instalment(
            paymentDate = nextPaymentDate,
            amount      = min(maxPaymentAmount, balance + latePaymentInterestAmount),
            interest    = latePaymentInterestAmount
          )
          val updatedInstalments = instalmentsAggregator :+ instalment

          val remainingPaymentDates = regularPaymentDates.drop(1)
          val remainingBalance = payablesUpdated(maxPaymentAmount, payables, maybeLatePaymentInterest).balance

          if (remainingBalance > 0 && remainingPaymentDates.isEmpty) {
            None
          } else {
            regularInstalmentsRecursive(
              planStartDate,
              remainingPaymentDates,
              maxPaymentAmount,
              payablesUpdated(maxPaymentAmount, payables, maybeLatePaymentInterest),
              dateToRate,
              updatedInstalments
            )
          }
      }
    }
  }

  private def latePaymentInterest(
      paymentAmount: BigDecimal,
      paymentDate:   LocalDate,
      payables:      Payables,
      dateToRate:    LocalDate => InterestRate,
      planStartDate: LocalDate
  ): Option[LatePaymentInterest] = {
    if (payables.inDate(paymentDate)) {
      None
    } else {
      val latePayments = Payables.latePayments(paymentAmount, paymentDate, payables)

      val latePaymentsInterestTotal = latePayments.map { p =>
        val currentInterestRate = dateToRate(p.dueDate).rate
        val currentDailyRate = currentInterestRate / BigDecimal(Year.of(p.dueDate.getYear).length()) / BigDecimal(100)
        val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(p.dueDate, p.payment.date))
        p.payment.amount * currentDailyRate * daysInterestToCharge
      }.sum

      Some(LatePaymentInterest(latePaymentsInterestTotal))
    }
  }

  private def payablesUpdated(paymentAmount: BigDecimal, payables: Payables, maybeInterest: Option[LatePaymentInterest]): Payables = {
    val liabilitiesWithLatePaymentInterest = maybeInterest match {
      case Some(interest) => payables.liabilities :+ interest
      case None           => payables.liabilities
    }
    val updatedLiabilities = liabilitiesWithLatePaymentInterest.foldLeft((paymentAmount, Seq.empty[Payable])) {
      case ((amount, newSeqBuilder), liability) if amount <= 0 =>
        (amount, newSeqBuilder :+ liability)

      case ((amount, newSeqBuilder), liability) if amount >= liability.amount =>
        (amount - liability.amount, newSeqBuilder)

      case ((paymentAmount, newSeqBuilder), TaxLiability(amount, dueDate)) =>
        (0, newSeqBuilder :+ TaxLiability(amount - paymentAmount, dueDate))

      case ((amount, newSeqBuilder), LatePaymentInterest(interest)) =>
        (0, newSeqBuilder :+ LatePaymentInterest(interest - amount))
    }._2
    Payables(updatedLiabilities)
  }

  private def min(amountA: BigDecimal, amountB: BigDecimal): BigDecimal = {
    if (amountA < amountB) {
      amountA
    } else {
      amountB
    }
  }

  def computeSchedule(journey: Journey)(implicit request: Request[_]): PaymentSchedule = {
    val availableSchedules: Seq[PaymentSchedule] = availablePaymentSchedules(
      journey.taxpayer.selfAssessment,
      journey.safeInitialPayment,
      journey.maybeArrangementDayOfMonth
    )

    val durationInMonths: Int = journey.calculatorDuration

    val schedule: PaymentSchedule = availableSchedules.find(_.instalments.length == durationInMonths)
      .orElse(
        availableSchedules.find(_.instalments.length == durationInMonths - 1)
      ).getOrElse(
          throw new RuntimeException(s"Could not find schedule corresponding to $durationInMonths [${journey}] [${availableSchedules}]")
        )
    schedule
  }

  def availablePaymentSchedules(sa: SelfAssessmentDetails, initialPayment: BigDecimal = BigDecimal(0),
                                maybeArrangementDayOfMonth: Option[ArrangementDayOfMonth])
    (implicit request: Request[_]): List[PaymentSchedule] = {

    val rangeOfAvailableScheduleDurationsInMonths = minimumMonthsAllowedTTP to 13

    val today: LocalDate = clockProvider.nowDate()

    val isAfterTaxYearEndDate = today.isAfter(today.withMonth(4).withDayOfMonth(5))
    val thresholdDate = today
      .plusYears(if (isAfterTaxYearEndDate) 2 else 1)
      .withMonth(1)
      .withDayOfMonth(29) // the last available payment can happen on 28 Jan next year

    val debits = sa.debits.map(asDebitInput)
    rangeOfAvailableScheduleDurationsInMonths.map { durationInMonths =>

      val defaultPreferredDayOfMonth = today
        .plusDays(`14 day gap between the initial payment date and the first scheduled payment date`)
        .plusDays(defaultInitialPaymentDays)
        .getDayOfMonth

      val dayOfMonth = maybeArrangementDayOfMonth.map(_.dayOfMonth).getOrElse(defaultPreferredDayOfMonth)

      val calculatorInput: TaxPaymentPlan = CalculatorService.changePaymentPlan(
        durationInMonths,
        dayOfMonth,
        initialPayment,
        debits
      )
      buildSchedule(calculatorInput)
    }
      .filter(_.lastPaymentDate.isBefore(thresholdDate))
      .filter(_.instalments.length <= 12) //max 12 instalments
      .toList
  }

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def buildSchedule(implicit taxPaymentPlan: TaxPaymentPlan): PaymentSchedule = {
    // Builds a seq of seq debits,
    // where each sub seq of debits is built from the different interest rate boundaries a debit crosses
    val overallDebits: Seq[Seq[Debit]] =
      taxPaymentPlan
        .liabilities
        .filter(_.dueDate.isBefore(taxPaymentPlan.startDate))
        .map(processDebit)

    // Calculate interest on old debits that have incurred interest up to the point of the current calculation date (today)
    val totalHistoricInterest: BigDecimal = (for {
      debit <- overallDebits.map(_.filterNot(_.dueDate.isAfter(taxPaymentPlan.startDate)))
    } yield calculateHistoricInterest(debit)).sum

    // Calculate interest for the initial days leading up until when the initial upfront payment is actually taken out of the taxpayer's account
    val hasAnInitialPayment: Boolean = taxPaymentPlan.initialPayment > 0
    val initialPaymentInterest: BigDecimal = if (hasAnInitialPayment) {
      val initialPaymentDate: LocalDate = taxPaymentPlan.startDate.plusDays(defaultInitialPaymentDays)
      val debitsDueBeforeInitialPayment: Seq[TaxLiability] = taxPaymentPlan.liabilities.filter(_.dueDate.isBefore(initialPaymentDate))
      calculateInitialPaymentInterest(debitsDueBeforeInitialPayment)
    } else {
      BigDecimal(0)
    }

    // Calculate the schedule of regular payments on the all debits due before endDate
    val instalments: Seq[Instalment] = createStagedPayments

    // Total amount of debt without interest
    val amountToPay = taxPaymentPlan.liabilities.map(_.amount).sum

    val totalInterest = (instalments.map(_.interest).sum + totalHistoricInterest + initialPaymentInterest).setScale(2, HALF_UP)

    PaymentSchedule(
      startDate            = taxPaymentPlan.startDate,
      endDate              = instalments.last.paymentDate.plusDays(LastPaymentDelayDays), // OPS-7952
      initialPayment       = taxPaymentPlan.initialPayment,
      amountToPay          = amountToPay,
      instalmentBalance    = amountToPay - taxPaymentPlan.initialPayment,
      totalInterestCharged = totalInterest,
      totalPayable         = amountToPay + totalInterest,
      instalments          = instalments.init :+ Instalment(
        instalments.last.paymentDate,
        instalments.last.amount + totalInterest,
        instalments.last.interest
      )
    )
  }

  def createStagedPayments(implicit taxPaymentPlan: TaxPaymentPlan): Seq[Instalment] = {
    val liabilities = taxPaymentPlan.outstandingLiabilities
    val repaymentDates: Seq[LocalDate] = durationService.getRepaymentDates(taxPaymentPlan.actualStartDate, taxPaymentPlan.endDate)
    val monthlyRepayment = taxPaymentPlan.monthlyRepayment(repaymentDates.size)
    createInstalments(liabilities, monthlyRepayment, repaymentDates)
  }

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def createInstalments(liabilities: Seq[TaxLiability], monthlyRepayment: BigDecimal, repaymentDates: Seq[LocalDate]): Seq[Instalment] = {
    val result = repaymentDates.foldLeft((liabilities, Seq.empty[Instalment])){
      case ((Nil, s), _) => (Nil, s)

      case ((ls, s), dt) if !ls.head.hasInterestCharge(dt) =>
        (amortizedLiabilities(ls, monthlyRepayment), s :+ Instalment(dt, monthlyRepayment, 0))

      case ((ls, s), dt) =>
        (amortizedLiabilities(ls, monthlyRepayment), s :+ Instalment(dt, monthlyRepayment, latePaymentInterest(latePayments(Payment(dt, monthlyRepayment))(ls))))
    }
    result._2
  }

  def latePaymentInterest(latePayments: Seq[LatePayment]): BigDecimal = {
    latePayments.map{ p =>
      val currentInterestRate = interestService.rateOn(p.dueDate).rate
      val currentDailyRate = currentInterestRate / BigDecimal(Year.of(p.dueDate.getYear).length()) / BigDecimal(100)
      val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(p.dueDate, p.payment.date))
      p.payment.amount * currentDailyRate * daysInterestToCharge
    }.sum

  }

  /**
   * Calculate interest for the initial payment amount for the first 7 days until the initial payment is taken out of the taxpayer's account.
   *
   * @param debits - only debits that are not after calculation date plus a week
   */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def calculateInitialPaymentInterest(debits: Seq[TaxLiability])(implicit calculation: TaxPaymentPlan): BigDecimal = {
    val currentInterestRate =
      interestService
        .rateOn(calculation.startDate)
        .rate

    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculation.startDate.getYear).length()) / BigDecimal(100)
    val sortedDebits: Seq[TaxLiability] = debits.sortBy(_.dueDate)

      def processDebits(amount: BigDecimal, debits: Seq[TaxLiability]): BigDecimal = {
        debits.toList match {
          case List(debit) => calculateAmount(amount, debit)._1 * calculateDays(debit) * currentDailyRate
          case debit :: remaining =>
            val result = calculateAmount(amount, debit)
            processDebits(result._2, remaining) + (result._1 * calculateDays(debit) * currentDailyRate)
          case Nil => 0
        }
      }

      def calculateDays(debit: TaxLiability): Long = {
        if (debit.dueDate.isBefore(calculation.startDate))
          defaultInitialPaymentDays
        else
          durationService.getDaysBetween(debit.dueDate, calculation.startDate.plusWeeks(1))
      }

      // Return - amount (used in the calculation), remaining downPayment
      def calculateAmount(amount: BigDecimal, debit: TaxLiability): (BigDecimal, BigDecimal) = {
        if (amount > debit.amount) {
          (debit.amount, amount - debit.amount)
        } else {
          (amount, 0)
        }
      }

    val initPaymentInterest = processDebits(calculation.initialPayment, sortedDebits)
    logger.info(s"InitialPayment Interest: $initPaymentInterest")
    initPaymentInterest
  }

  /**
   * Get the historic interest rates that should be applied to a given debit and split the debit
   * into multiple debits, covering each interest rate.
   */
  private def processDebit(debit: TaxLiability)(implicit calculation: TaxPaymentPlan): Seq[Debit] = {
    interestService.getRatesForPeriod(
      debit.dueDate,
      calculation.endDate
    ).map { rate =>
        (rate.containsDate(debit.dueDate), rate.containsDate(calculation.endDate)) match {
          case DebitDueAndCalculationDatesWithinRate => Debit(
            amount  = debit.amount,
            dueDate = debit.dueDate,
            endDate = calculation.endDate,
            rate    = rate
          )
          case DebitDueDateWithinRate => Debit(
            amount  = debit.amount,
            dueDate = debit.dueDate,
            endDate = rate.endDate,
            rate    = rate
          )
          case CalculationDateWithinRate => Debit(
            amount  = debit.amount,
            dueDate = rate.startDate,
            endDate = calculation.endDate,
            rate    = rate
          )
          case _ => Debit(
            amount  = debit.amount,
            dueDate = rate.startDate,
            endDate = rate.endDate,
            rate    = rate
          )
        }
      }
  }

  /**
   * Calculate the amount of historic interest on liable debits, taking into account whether
   * the number of days between two dates is inclusive (count one of the dates) or double
   * inclusive (count both days).
   */
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  private def calculateHistoricInterest(debits: Seq[Debit])(implicit calculation: TaxPaymentPlan): BigDecimal = {
    debits.map { debit =>
      val debitRateEndDate = debit.rate.endDate
      val inclusive = if (!(debits.head.equals(debit) | debits.last.equals(debit))) 1 else 0
      val endDate = historicRateEndDate(debitRateEndDate)

      val numberOfDays = durationService.getDaysBetween(debit.dueDate, endDate) + inclusive
      val historicRate = debit.historicDailyRate
      val total = historicRate * debit.amount * numberOfDays

      logger.info(s"Historic interest: rate $historicRate days $numberOfDays amount ${debit.amount} total = $total")
      logger.info(s"Debit due date: ${debit.dueDate} and end date: $endDate is inclusive: $inclusive")
      logger.info(s"Debit Rate date: $debitRateEndDate and calculation start date: ${calculation.startDate}")
      total
    }.sum
  }

  /**
   * Calculate the amount of historic interest on liable debits, taking into account whether
   * the number of days between two dates is inclusive (count one of the dates) or double
   * inclusive (count both days).
   */
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  private def calculateHistoricInterest(debits: Seq[FixedInterestPeriod])(implicit paymentsCalendar: PaymentsCalendar): BigDecimal = {
    debits.map { debit =>
      val debitRateEndDate = debit.rate.endDate
      val inclusive = if (!(debits.head.equals(debit) | debits.last.equals(debit))) 1 else 0
      val endDate = historicRateEndDateNew(debitRateEndDate)

      val numberOfDays = durationService.getDaysBetween(debit.dueDate, endDate) + inclusive
      val historicRate = debit.historicDailyRate
      val total = historicRate * debit.amountAccruingInterest * numberOfDays

      logger.info(s"Historic interest: rate $historicRate days $numberOfDays amount ${debit.amountAccruingInterest} total = $total")
      logger.info(s"Debit due date: ${debit.dueDate} and end date: $endDate is inclusive: $inclusive")
      logger.info(s"Debit Rate date: $debitRateEndDate and calculation start date: ${paymentsCalendar.planStartDate}")
      total
    }.sum
  }

  private def historicRateEndDateNew(debitEndDate: LocalDate)(implicit paymentsCalendar: PaymentsCalendar): LocalDate =
    if (debitEndDate.getYear.equals(paymentsCalendar.planStartDate.getYear)) paymentsCalendar.planStartDate else debitEndDate

  private def historicRateEndDate(debitEndDate: LocalDate)(implicit calculation: TaxPaymentPlan): LocalDate =
    if (debitEndDate.getYear.equals(calculation.startDate.getYear)) calculation.startDate else debitEndDate
}

object CalculatorService {
  private val latestValidPaymentDayOfMonth = 28

  def makeCalculatorInputForPayToday(debits: Seq[TaxLiability])(implicit clock: Clock): TaxPaymentPlan = {

    val taxPaymentPlan =
      changePaymentPlan(
        durationInMonths           = 0,
        preferredPaymentDayOfMonth = now(clock).getDayOfMonth,
        initialPayment             = 0,
        debits                     = debits
      )
    taxPaymentPlan
  }

  def makeTaxPaymentPlan(debits: Seq[TaxLiability], initialPayment: BigDecimal, durationInMonths: Int)
    (implicit clock: Clock): TaxPaymentPlan = {

    val noInitialPayment = BigDecimal(0)
    val workingDaysInAWeek = 5

    val currentDate = now(clock)
    val endDate = currentDate.plusMonths(durationInMonths)
    val possibleFirstPaymentDate = addWorkingDays(currentDate, workingDaysInAWeek)

    val firstPaymentDate: LocalDate =
      if (possibleFirstPaymentDate.getDayOfMonth > latestValidPaymentDayOfMonth)
        possibleFirstPaymentDate.withDayOfMonth(1).plusMonths(1)
      else
        possibleFirstPaymentDate

    if (initialPayment > 0) {
      val deferredEndDate = endDate.plusMonths(1)
      val deferredFirstPaymentDate = firstPaymentDate.plusMonths(1)

      if ((debits.map(_.amount).sum - initialPayment) < BigDecimal.exact("32.00")) {
        TaxPaymentPlan(
          startDate        = currentDate,
          initialPayment   = noInitialPayment,
          firstPaymentDate = Some(deferredFirstPaymentDate),
          endDate          = deferredEndDate,
          liabilities      = debits)
      } else {
        TaxPaymentPlan(
          startDate        = currentDate,
          initialPayment   = initialPayment,
          firstPaymentDate = Some(deferredFirstPaymentDate),
          endDate          = deferredEndDate,
          liabilities      = debits)
      }
    } else //no initial payment
      TaxPaymentPlan(
        startDate        = currentDate,
        initialPayment   = noInitialPayment,
        endDate          = endDate,
        firstPaymentDate = Some(firstPaymentDate),
        liabilities      = debits
      )
  }

  /**
   * As the user can change which day of the month they wish to make their payments, then a recalculation
   * must be made as this would effect the interest amounts. Rules here must be applied and this function
   * calculates the first payment date and the last payment date by applying these rules.
   *
   * Rules:
   * - First payment must be at least 7 days from today's date
   * - The day of the month cannot be greater than 28, if it is then use the 1st of the following month
   * - There must be at least a 14 day gap between the initial payment date and the first scheduled payment date
   */
  def changePaymentPlan(
      durationInMonths:           Int,
      preferredPaymentDayOfMonth: Int,
      initialPayment:             BigDecimal,
      debits:                     Seq[TaxLiability])
    (implicit clock: Clock): TaxPaymentPlan = {

    /*
     * We add 10 days extra capacity just in case if there are 3 bank holidays within the 10 days
     * so ETMP still has chance to create direct debit instruction within 2 working days
     * (it's assumed that weekends aren't working days)
     */
    val fewDays = 10

    val oneWeek = 7
    val twoWeeks = oneWeek * 2

    val startDate = now(clock)
    val startDatePlusOneWeek = startDate.plusWeeks(1)

    val bestMatchingPaymentDayOfMonth = if (preferredPaymentDayOfMonth > latestValidPaymentDayOfMonth) 1 else preferredPaymentDayOfMonth
    val defaultPaymentDate = startDate.withDayOfMonth(bestMatchingPaymentDayOfMonth)
    val defaultPaymentDatePlusOneMonth = defaultPaymentDate.plusMonths(1)
    val defaultPaymentDatePlusTwoMonths = defaultPaymentDate.plusMonths(2)

    val defaultEndDate = defaultPaymentDate.plusMonths(durationInMonths)
    val defaultEndDatePlusOneMonth = defaultEndDate.plusMonths(1)
    val defaultEndDatePlusTwoMonths = defaultEndDate.plusMonths(2)

    val (firstPaymentDate: LocalDate, endDate: LocalDate) =
      if (initialPayment.equals(BigDecimal(0))) {
        val daysBetweenStartAndPaymentDates = DAYS.between(startDate, defaultPaymentDate)

        if (daysBetweenStartAndPaymentDates < fewDays && DAYS.between(startDate, defaultPaymentDatePlusOneMonth) < fewDays)
          (defaultPaymentDatePlusTwoMonths, defaultEndDatePlusTwoMonths)
        else if (daysBetweenStartAndPaymentDates < fewDays)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else
          (defaultPaymentDate, defaultEndDate)
      } else {
        val paymentDateWithinAWeekOfStartDate = defaultPaymentDate.isBefore(startDatePlusOneWeek)

        if (paymentDateWithinAWeekOfStartDate && DAYS.between(startDatePlusOneWeek, defaultPaymentDatePlusOneMonth) < twoWeeks)
          (defaultPaymentDatePlusTwoMonths, defaultEndDatePlusTwoMonths)
        else if (paymentDateWithinAWeekOfStartDate)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else if (DAYS.between(startDatePlusOneWeek, defaultPaymentDate) < twoWeeks)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else
          (defaultPaymentDate, defaultEndDate)
      }

    TaxPaymentPlan(debits, initialPayment, startDate, endDate.minusDays(1), Some(firstPaymentDate))
  }

  val minimumMonthsAllowedTTP = 2

  implicit def ordered[A](implicit ev$1: A => Comparable[_ >: A]): Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }

}
