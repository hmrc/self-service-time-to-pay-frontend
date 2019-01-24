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

package views.selfservicetimetopay.helpers.forms

import play.api.i18n.Messages
import play.twirl.api.Html
import views.html.helper.{FieldConstructor, FieldElements}
import views.html.selfservicetimetopay.helpers.forms.input_field_constructor


object FieldConstructors {
  private def inputFieldConstructorFunction: FieldElements => Html =
    (elements: FieldElements) => input_field_constructor(elements)

  implicit val customInputFieldConstructor: FieldConstructor =
    FieldConstructor(inputFieldConstructorFunction)
}
