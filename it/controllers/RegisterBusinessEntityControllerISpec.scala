/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import assets.TestConstants._
import play.api.libs.json.Json
import play.api.test.Helpers._
import stubs.{AuthStub, RegisterWithMultipleIdentifiersStub}
import utils.ComponentSpecHelper

class RegisterBusinessEntityControllerISpec extends ComponentSpecHelper with AuthStub with RegisterWithMultipleIdentifiersStub {

  "POST /register-limited-company" should {
    "return OK with status Registered and the SafeId" when {
      "the Registration was a success" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testRegisterCompanyJsonBody, testRegime)(OK, testSafeId)
        val jsonBody = Json.obj(
          "crn" -> testCompanyNumber,
          "ctutr" -> testCtutr,
          "regime" -> testRegime
        )
        val resultJson = Json.obj(
          "registration" -> Json.obj(
            "registrationStatus" -> "REGISTERED",
            "registeredBusinessPartnerId" -> testSafeId))

        val result = post("/register-limited-company")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson
      }
    }
    "return REGISTRATION_FAILED" when {
      "the Registration was not successful" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersFailure(testRegisterCompanyJsonBody, testRegime)(BAD_REQUEST)
        val jsonBody = Json.obj(
          "crn" -> testCompanyNumber,
          "ctutr" -> testCtutr,
          "regime" -> testRegime
        )
        val resultJson = Json.obj(
          "registration" -> Json.obj(
            "registrationStatus" -> "REGISTRATION_FAILED",
          "failures" -> Json.arr(testRegisterResponseFailureBody))
          )
        val result = post("/register-limited-company")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson
      }
    }
  }
  "POST /register-registered-society" should {
    "return OK with status Registered and the SafeId" when {
      "the Registration was a success" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testRegisterRegisteredSocietyJsonBody, testRegime)(OK, testSafeId)
        val jsonBody =
          Json.obj(
            "crn" -> testCompanyNumber,
            "ctutr" -> testCtutr,
            "regime" -> testRegime
          )
        val resultJson = Json.obj(
          "registration" -> Json.obj(
            "registrationStatus" -> "REGISTERED",
            "registeredBusinessPartnerId" -> testSafeId))

        val result = post("/register-registered-society")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson
      }
    }
    "return REGISTRATION_FAILED" when {
      "the Registration was not successful" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersFailure(testRegisterRegisteredSocietyJsonBody, testRegime)(BAD_REQUEST)

        val jsonBody =
          Json.obj(
            "crn" -> testCompanyNumber,
            "ctutr" -> testCtutr,
            "regime" -> testRegime
          )


        val resultJson = Json.obj(
          "registration" -> Json.obj(
            "registrationStatus" -> "REGISTRATION_FAILED",
            "failures" -> Json.arr(testRegisterResponseFailureBody))
        )

        val result = post("/register-registered-society")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson
      }
    }
  }

}
