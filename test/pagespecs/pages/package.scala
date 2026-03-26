/*
 * Copyright 2026 HM Revenue & Customs
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

package pagespecs

package object pages {

  implicit class StringOps(private val s: String) extends AnyVal {
    /**
     * Transforms string so it's easier it to compare.
     * It also replaces `unchecked`
     *
     */
    def stripSpaces(): String = s
      .replaceAll("unchecked", "") //when you run tests from intellij webdriver.getText adds extra 'unchecked' around selection
      .replaceAll("[^\\S\\r\\n]+", " ") //replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") //replace many consecutive new lines with one new line
      .split("\n").map(_.trim) //trim each line
      .filterNot(_ == "") //remove any empty lines
      .mkString("\n")

    def splitIntoLines(): Seq[String] = s
      .stripSpaces()
      .split("\n")
      .toIndexedSeq
  }

}
