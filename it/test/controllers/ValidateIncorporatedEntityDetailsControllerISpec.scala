/*
 * Copyright 2024 HM Revenue & Customs
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

import assets.TestConstants.*
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import play.api.libs.json.Json
import play.api.test.Helpers.*
import stubs.{AuthStub, GetCtReferenceStub}
import utils.ComponentSpecHelper

class ValidateIncorporatedEntityDetailsControllerISpec extends ComponentSpecHelper with GetCtReferenceStub with AuthStub {

  private val expectedDetailsMismatchedJson = Json.obj("matched" -> false)

  "validateDetails" should {
    "return details match" when {
      "supplied details match those in database" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubGetCtReference(testCompanyNumber)(status = OK, body = Json.obj("CTUTR" -> testCtutr))
        val expectedJson = Json.obj("matched" -> true)
        val suppliedJson = Json.obj(
          "companyNumber" -> testCompanyNumber,
          "ctutr"         -> testCtutr
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe OK
        result.json mustBe expectedJson
      }
    }

    "return details do not match" when {
      "supplied details do not match those in database" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubGetCtReference(testCompanyNumber)(status = OK, body = Json.obj("CTUTR" -> testCtutr))

        val suppliedJson = Json.obj(
          "companyNumber" -> testCompanyNumber,
          "ctutr"         -> "mismatch"
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe OK
        result.json mustBe expectedDetailsMismatchedJson
      }

      "the user asserts that the unincorporated association does not have a Ct Utr, but one is found" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubGetCtReference(testCompanyNumber)(status = OK, body = Json.obj("CTUTR" -> testCtutr))

        val suppliedJson = Json.obj("companyNumber" -> testCompanyNumber)

        val result = post(uri = "/validate-details")(suppliedJson)

        result.status mustBe OK
        result.json mustBe expectedDetailsMismatchedJson
      }
    }

    "return details not found" when {

      "supplied details are not found in database" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubGetCtReference("000000000")(status = NOT_FOUND)

        val expectedJson = Json.obj(
          "code"   -> "NOT_FOUND",
          "reason" -> "HoD has indicated that CT UTR cannot be returned on GET <http://localhost:11111/corporation-tax/identifiers/crn/000000000>"
        )

        val suppliedJson = Json.obj(
          "companyNumber" -> "000000000",
          "ctutr"         -> testCtutr
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe BAD_REQUEST
        result.json mustBe expectedJson
      }

    }

    "delegates error when downstream has failed" when {

      "corporate-tax returns OK but body is a html (this happens in real life therefore a test to cover it)" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        wireMockServer.stubFor(
          WireMock
            .get(urlPathMatching(s"/corporation-tax/identifiers/crn/.*"))
            .willReturn(okJson("<html></html>"))
        )

        val expectedJson = Json.obj(
          "code" -> "BAD_GATEWAY",
          "reason" -> "HoD returned a malformed JSON on GET <http://localhost:11111/corporation-tax/identifiers/crn/000000000> errors: Unexpected character ('<' (code 60)): ex"
        )
        val suppliedJson = Json.obj(
          "companyNumber" -> "000000000",
          "ctutr"         -> testCtutr
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe BAD_GATEWAY
        result.json mustBe expectedJson
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(s"/corporation-tax/identifiers/crn/000000000")))
      }

      "corporate-tax returns error" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        wireMockServer.stubFor(
          WireMock
            .get(urlPathMatching(s"/corporation-tax/identifiers/crn/.*"))
            .willReturn(aResponse.withStatus(502))
        )

        val expectedJson = Json.obj(
          "code"   -> "BAD_GATEWAY",
          "reason" -> "HoD returned status code <502> on GET <http://localhost:11111/corporation-tax/identifiers/crn/000000000>"
        )

        val suppliedJson = Json.obj(
          "companyNumber" -> "000000000",
          "ctutr"         -> testCtutr
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe BAD_GATEWAY
        result.json mustBe expectedJson
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(s"/corporation-tax/identifiers/crn/000000000")))
      }
    }

    "return Unauthorised" when {
      "there is an auth failure" in {
        stubAuthFailure()

        val suppliedJson = Json.obj(
          "companyNumber" -> testCompanyNumber,
          "ctutr"         -> testCtutr
        )

        val result = post("/validate-details")(suppliedJson)

        result.status mustBe UNAUTHORIZED

      }
    }
  }

}
