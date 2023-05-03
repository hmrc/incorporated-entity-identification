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

package assets

import play.api.libs.json.{JsObject, Json}

import java.util.UUID

object TestConstants {

  val testJourneyId = "1234567"
  val testCompanyNumber = "12345678"
  val testCompanyName = "Test Company Ltd"
  val testCtutr = "1234567890"
  val testRegime = "VATC"
  val testInternalId: String = UUID.randomUUID().toString
  val testSafeId: String = UUID.randomUUID().toString
  val testRegisterCompanyJsonBody: JsObject = Json.obj(
    "company" ->
      Json.obj(
        "crn"   -> testCompanyNumber,
        "ctutr" -> testCtutr
      )
  )
  val testRegisterRegisteredSocietyJsonBody: JsObject = Json.obj(
    "registeredSociety" ->
      Json.obj(
        "crn"   -> testCompanyNumber,
        "ctutr" -> testCtutr
      )
  )

  val testCode: String = "INVALID_PAYLOAD"
  val testReason: String = "Request has not passed validation. Invalid payload."

  val testRegisterResponseFailureBody: JsObject =
    Json.obj(
      "code"   -> testCode,
      "reason" -> testReason
    )

  val registerResponseMultipleFailureBody: JsObject =
    Json.obj(
      "failures" -> Json.arr(
        Json.obj(
          "code"   -> "INVALID_PAYLOAD",
          "reason" -> "Request has not passed validation. Invalid payload."
        ),
        Json.obj(
          "code"   -> "INVALID_REGIME",
          "reason" -> "Request has not passed validation.  Invalid regime."
        )
      )
    )

}
