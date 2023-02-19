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

package views

import javax.inject.Inject

class Views @Inject() (
    val create_user_and_log_in:                        views.html.testonly.create_user_and_log_in,
    val payment_today_question:                        views.html.calculator.payment_today_question,
    val payment_today_form:                            views.html.calculator.payment_today_form,
    val check_you_can_afford:                          views.html.affordability.check_you_can_afford,
    val add_income_spending:                           views.html.affordability.add_income_spending,
    val your_monthly_income:                           views.html.affordability.your_monthly_income,
    val your_monthly_spending:                         views.html.affordability.your_monthly_spending,
    val how_much_you_could_afford:                     views.html.affordability.how_much_you_could_afford,
    val we_cannot_agree_your_pp:                       views.html.affordability.we_cannot_agree_your_pp,
    val calculate_instalments_form:                    views.html.calculator.calculate_instalments_form,
    val payment_summary:                               views.html.calculator.payment_summary,
    val tax_liabilities:                               views.html.calculator.tax_liabilities,
    val about_bank_account:                            views.html.arrangement.about_bank_account,
    val direct_debit_confirmation:                     views.html.arrangement.direct_debit_confirmation,
    val application_complete:                          views.html.arrangement.application_complete,
    val direct_debit_assistance:                       views.html.arrangement.direct_debit_assistance,
    val change_day:                                    views.html.arrangement.change_day,
    val instalment_plan_summary:                       views.html.arrangement.instalment_plan_summary, //delete after completing OPS-8645
    val check_payment_plan:                            views.html.arrangement.check_payment_plan,
    val direct_debit_form:                             views.html.arrangement.direct_debit_form,
    val direct_debit_unauthorised:                     views.html.arrangement.direct_debit_unauthorised,
    val terms_and_conditions:                          views.html.arrangement.terms_and_conditions,
    val view_payment_plan:                             views.html.arrangement.view_payment_plan,
    val inspector:                                     views.html.testonly.inspector,
    val not_enrolled:                                  views.html.core.not_enrolled,
    val not_sole_signatory:                            views.html.core.not_sole_signatory,
    val you_need_to_request_access_to_self_assessment: views.html.core.you_need_to_request_access_to_self_assessment,
    val debt_too_large:                                views.html.core.debt_too_large,
    val service_start:                                 views.html.core.service_start,
    val call_us:                                       views.html.core.call_us,
    val you_need_to_file:                              views.html.core.you_need_to_file,

    //    val print_payment_schedule:       views.html.partials.print_payment_schedule,

    val delete_answers: views.html.session.delete_answers
)

