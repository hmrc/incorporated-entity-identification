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

package controllers

import assets.TestConstants.{testCompanyNumber, testCtutr, testInternalId, testSafeId}
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import stubs.{AuthStub, RegisterWithMultipleIdentifiersStub}
import utils.ComponentSpecHelper

class RegisterWithMultipleIdentifiersControllerISpec extends ComponentSpecHelper with AuthStub with RegisterWithMultipleIdentifiersStub {

  "POST /cross-regime/register/VATC" should {
    "return OK with status Registered and the SafeId" when {
      "the Registration was a success" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testCompanyNumber, testCtutr)(200, testSafeId)

        val jsonBody = Json.obj(
          "company" ->
            Json.obj(
              "crn" -> testCompanyNumber,
              "ctutr" -> testCtutr
            )
        )

        val resultJson = Json.obj(
          "registration" -> Json.obj(
            "registrationStatus" -> "REGISTERED",
            "registeredBusinessPartnerId" -> testSafeId))

        val result = post(s"/cross-regime/register/VATC")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson
      }
    }
    "return OK with status Registration_Failed" when {
      "the Registration was not successful" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersFailure(testCompanyNumber, testCtutr)(400)

        val jsonBody = Json.obj(
          "company" ->
            Json.obj(
              "crn" -> testCompanyNumber,
              "ctutr" -> testCtutr
            )
        )

        val resultJson = Json.obj(
          "registration" -> Json.obj(
            "registrationStatus" -> "REGISTRATION_FAILED"))

        val result = post(s"/cross-regime/register/VATC")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson
      }
    }
  }

}
