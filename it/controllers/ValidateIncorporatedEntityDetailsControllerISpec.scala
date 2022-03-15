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
import stubs.{AuthStub, GetCtReferenceStub}
import utils.ComponentSpecHelper

class ValidateIncorporatedEntityDetailsControllerISpec extends ComponentSpecHelper with GetCtReferenceStub with AuthStub {

  private val expectedNoFoundResponseJson = Json.obj(
    "code" -> "NOT_FOUND",
    "reason" -> "The back end has indicated that CT UTR cannot be returned"
  )

  "validateDetails" should {
    "return details match" when {
      "supplied details match those in database" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubGetCtReference(testCompanyNumber)(status = OK, body = Json.obj("CTUTR" -> testCtutr))
        val testJson = Json.obj("matched" -> true)
        val suppliedJson = Json.obj(
          "companyNumber" -> testCompanyNumber,
          "ctutr" -> testCtutr
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe OK
        result.json mustBe testJson
      }
    }

    "return details do not match" when {
      "supplied details do not match those in database" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubGetCtReference(testCompanyNumber)(status = OK, body = Json.obj("CTUTR" -> testCtutr))
        val testJson = Json.obj("matched" -> false)
        val suppliedJson = Json.obj(
          "companyNumber" -> testCompanyNumber,
          "ctutr" -> "mismatch"
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe OK
        result.json mustBe testJson
      }
    }

    "return details not found" when {

      "supplied details are not found in database" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubGetCtReference("000000000")(status = NOT_FOUND)

        val suppliedJson = Json.obj(
          "companyNumber" -> "000000000",
          "ctutr" -> testCtutr
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe BAD_REQUEST
        result.json mustBe expectedNoFoundResponseJson
      }

      "the user asserts that the unincorporated association does not have a Ct Utr, but one is found" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubGetCtReference(testCompanyNumber)(status = OK, body = Json.obj("CTUTR" -> testCtutr))

        val suppliedJson = Json.obj("companyNumber" -> testCompanyNumber)

        val result = post(uri = "/validate-details")(suppliedJson)

        result.status mustBe BAD_REQUEST
        result.json mustBe expectedNoFoundResponseJson
      }
    }
    "return Unauthorised" when {
      "there is an auth failure" in {
        stubAuthFailure()

        val suppliedJson = Json.obj(
          "companyNumber" -> testCompanyNumber,
          "ctutr" -> testCtutr
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe UNAUTHORIZED

      }
    }
  }

}
