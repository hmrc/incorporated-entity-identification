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

import assets.TestConstants._
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.incorporatedentityidentification.models.BusinessVerificationStatus._
import stubs.{AuthStub, RegisterWithMultipleIdentifiersStub}
import utils.{ComponentSpecHelper, JourneyDataHelper, JourneyDataMongoHelper}

class RegisterBusinessEntityControllerISpec extends ComponentSpecHelper with AuthStub with JourneyDataMongoHelper with RegisterWithMultipleIdentifiersStub {

  "POST /register-limited-company" should {
    "return OK with status Registered and the SafeId" when {
      "the Registration was a success" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testRegisterCompanyJsonBody, testRegime)(OK, testSafeId)
        val jsonBody = Json.obj(
          "crn"    -> testCompanyNumber,
          "ctutr"  -> testCtutr,
          "regime" -> testRegime
        )

        val result = post("/register-limited-company")(jsonBody)
        result.status mustBe OK
        result.json mustBe registrationSuccess
      }
    }
    "return REGISTRATION_FAILED" when {
      "the Registration was not successful" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersFailure(testRegisterCompanyJsonBody, testRegime)(BAD_REQUEST, testRegisterResponseFailureBody)
        val jsonBody = Json.obj(
          "crn"    -> testCompanyNumber,
          "ctutr"  -> testCtutr,
          "regime" -> testRegime
        )
        val resultJson = Json.obj(
          "registration" -> Json.obj("registrationStatus" -> "REGISTRATION_FAILED", "failures" -> Json.arr(testRegisterResponseFailureBody))
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
            "crn"    -> testCompanyNumber,
            "ctutr"  -> testCtutr,
            "regime" -> testRegime
          )

        val result = post("/register-registered-society")(jsonBody)
        result.status mustBe OK
        result.json mustBe registrationSuccess
      }
    }
    "return REGISTRATION_FAILED" when {
      "the Registration was not successful" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersFailure(testRegisterRegisteredSocietyJsonBody, testRegime)(BAD_REQUEST, testRegisterResponseFailureBody)

        val jsonBody =
          Json.obj(
            "crn"    -> testCompanyNumber,
            "ctutr"  -> testCtutr,
            "regime" -> testRegime
          )

        val resultJson = Json.obj(
          "registration" -> Json.obj("registrationStatus" -> "REGISTRATION_FAILED", "failures" -> Json.arr(testRegisterResponseFailureBody))
        )

        val result = post("/register-registered-society")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson
      }
    }
  }

  "POST /register-limited-company for VER-5018" should {

    "return OK with status Registered and the SafeId" when {

      "the Registration with business verification was a success" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationPassKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testRegisterCompanyJsonBody, testRegime)(OK, testSafeId)

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe OK
        result.json mustBe registrationSuccess

        verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

      "the registration was successful and the user passed business verification because they were Ct enrolled" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationCtEnrolledKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testRegisterCompanyJsonBody, testRegime)(OK, testSafeId)

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe OK
        result.json mustBe registrationSuccess

        verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }


      "the business verification flag is set to false" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          None,
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testRegisterCompanyJsonBody, testRegime)(OK, testSafeId)

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = false, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe OK
        result.json mustBe registrationSuccess

        verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

    }

    "return REGISTRATION_FAILED" when {

      "the Registration was not successful" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationPassKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersFailure(testRegisterCompanyJsonBody, testRegime)(BAD_REQUEST, testRegisterResponseFailureBody)

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val resultJson = Json.obj(
          "registration" -> Json.obj("registrationStatus" -> "REGISTRATION_FAILED", "failures" -> Json.arr(testRegisterResponseFailureBody))
        )

        val result = post("/register-limited-company")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson

        verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

    }

    "return REGISTRATION_NOT_CALLED" when {

      "business verification was not successful" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationFailKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val resultJson = Json.obj(
          "registration" -> Json.obj("registrationStatus" -> "REGISTRATION_NOT_CALLED")
        )

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe OK
        result.json mustBe resultJson

        verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

      "there was not enough information to challenge business verification" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationNotEnoughInfoToChallengeKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val resultJson = Json.obj(
          "registration" -> Json.obj("registrationStatus" -> "REGISTRATION_NOT_CALLED")
        )

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe OK
        result.json mustBe resultJson

        verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")

      }

      "there was not enough information to call business verification" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationNotEnoughInfoToCallBVKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val resultJson = Json.obj(
          "registration" -> Json.obj("registrationStatus" -> "REGISTRATION_NOT_CALLED")
        )

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe OK
        result.json mustBe resultJson

        verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")

      }

    }

    "return Unauthorized" when {

      "the request has not been authorized" in {

        stubAuthFailure()

        val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe UNAUTHORIZED
      }

    }

    "return Bad request" when {

      "the post data is incomplete" in {

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody: JsObject = Json.obj("regime" -> testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe BAD_REQUEST

      }

    }

    "return INTERNAL_SERVER_ERROR" when {

      "the business verification flag is set to true but the status is missing" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          None,
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe INTERNAL_SERVER_ERROR
        result.body mustBe s"Missing business verification state in database for journey $testJourneyId"

        verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

      "the company profile is missing" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = false,
          Some(businessVerificationPassKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe INTERNAL_SERVER_ERROR
        result.body mustBe s"Missing required data for registration in database for journey $testJourneyId"

        verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

      "the Ct Utr is missing" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationPassKey),
          None)

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe INTERNAL_SERVER_ERROR
        result.body mustBe s"Missing required data for registration in database for journey $testJourneyId"

        verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

    }

  }

  "POST /register-registered-society for VER-5018" should {

    "return OK with status Registered and the SafeId" when {

      "the Registration was a success" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationPassKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testRegisterRegisteredSocietyJsonBody, testRegime)(OK, testSafeId)

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-registered-society")(jsonBody)

        result.status mustBe OK
        result.json mustBe registrationSuccess

        verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

    }

    "return REGISTRATION_FAILED" when {

      "the registration was not successful" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationPassKey),
          Some(testCtutr))

        insertById(testJourneyId, testInternalId, testJourneyData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersFailure(testRegisterRegisteredSocietyJsonBody, testRegime)(BAD_REQUEST, testRegisterResponseFailureBody)

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val resultJson = Json.obj(
          "registration" -> Json.obj("registrationStatus" -> "REGISTRATION_FAILED", "failures" -> Json.arr(testRegisterResponseFailureBody))
        )

        val result = post("/register-registered-society")(jsonBody)
        result.status mustBe OK
        result.json mustBe resultJson

        verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }
    }

    "return Unauthorized" when {

      "the request has not been authorized" in {

        stubAuthFailure()

        val jsonBody = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-registered-society")(jsonBody)

        result.status mustBe UNAUTHORIZED

      }

    }

    "return Bad request" when {

      "the post data is incomplete" in {

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody: JsObject = Json.obj("regime" -> testRegime)

        val result = post("/register-registered-society")(jsonBody)

        result.status mustBe BAD_REQUEST

      }

    }

  }

  private def createJsonBody(journeyId: String, businessVerificationCheck: Boolean, regime: String): JsObject = {

    Json.obj(
      "journeyId" -> journeyId,
      "businessVerificationCheck" -> businessVerificationCheck,
      "regime" -> regime
    )

  }

}
