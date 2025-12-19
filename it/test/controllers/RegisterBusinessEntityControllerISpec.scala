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
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSBodyReadables.readableAsString
import play.api.test.Helpers.*
import stubs.{AuthStub, RegisterWithMultipleIdentifiersStub}
import uk.gov.hmrc.incorporatedentityidentification.models.BusinessVerificationStatus.*
import uk.gov.hmrc.incorporatedentityidentification.models.{Failure, Registered, RegistrationFailed, RegistrationNotCalled}
import utils.{ComponentSpecHelper, JourneyDataHelper, JourneyDataMongoHelper}

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
class RegisterBusinessEntityControllerISpec extends ComponentSpecHelper with AuthStub with JourneyDataMongoHelper with RegisterWithMultipleIdentifiersStub {

  val ec: ExecutionContext = ExecutionContext.global

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) match {
          case Some(registrationStatus) => registrationStatus match {
            case registrationFailed: RegistrationFailed =>
              registrationFailed.registrationFailures.size mustBe 1
              registrationFailed.registrationFailures.head mustBe Array(Failure(testCode, testReason))
            case _ => fail("Unexpected registration status retrieved")
          }
          case None => fail("Registration status not found")
        }

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(RegistrationNotCalled)

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(RegistrationNotCalled)

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(RegistrationNotCalled)

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

      "the response from auth does not contain an internal identifier" in {

        stubAuth(OK, emptyAuthResponse())

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

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

        retrieveRegistrationStatus(testJourneyId, testInternalId) match {
          case Some(registrationStatus) => registrationStatus match {
            case registrationFailed: RegistrationFailed =>
              registrationFailed.registrationFailures.size mustBe 1
              registrationFailed.registrationFailures.head mustBe Array(Failure(testCode, testReason))
            case _ => fail("Unexpected registration status retrieved")
          }
          case None => fail("Registration status not found")
        }

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

      "the response from auth does not contain an internal identifier" in {

        stubAuth(OK, emptyAuthResponse())

        val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

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

  "VER-5038 Registration submission retry mechanism" should {

    "return registration status from journey data when registration has been completed within registration timeout" in {

      val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
        testJourneyId,
        testInternalId,
        withCompanyData = true,
        Some(businessVerificationPassKey),
        Some(testCtutr)
      )

      val now: Instant = OffsetDateTime.now(ZoneOffset.UTC).toInstant

      val registrationTimestamp: Instant = Instant.ofEpochMilli(now.toEpochMilli - 1000)

      val testJourneyDataWithRegistrationStatusAndTimeout: JsObject = testJourneyData ++
        JourneyDataHelper.getRegistrationTimestamp(registrationTimestamp) ++
        JourneyDataHelper.getSuccessfulRegistrationStatus(testSafeId)

      insertById(testJourneyId, testInternalId, testJourneyDataWithRegistrationStatusAndTimeout)

      stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

      val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

      val result = post("/register-registered-society")(jsonBody)

      result.status mustBe OK
      result.json mustBe registrationSuccess

      retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

      verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")

    }

    "resubmit registration when registration has been completed but the timeout has expired" in {

      val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
        testJourneyId,
        testInternalId,
        withCompanyData = true,
        Some(businessVerificationPassKey),
        Some(testCtutr)
      )

      val now: Instant = OffsetDateTime.now(ZoneOffset.UTC).toInstant

      val registrationTimestamp: Instant = Instant.ofEpochMilli(now.toEpochMilli - 3000)

      val testJourneyDataWithRegistrationStatusAndTimeout: JsObject = testJourneyData ++
        JourneyDataHelper.getRegistrationTimestamp(registrationTimestamp) ++
        JourneyDataHelper.getSuccessfulRegistrationStatus(testSafeId)

      insertById(testJourneyId, testInternalId, testJourneyDataWithRegistrationStatusAndTimeout)

      stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
      stubRegisterWithMultipleIdentifiersSuccess(testRegisterRegisteredSocietyJsonBody, testRegime)(OK, testSafeId)

      val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

      val result = post("/register-registered-society")(jsonBody)

      result.status mustBe OK
      result.json mustBe registrationSuccess

      retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

      verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
    }

    "return registration data when registration completes during timeout using retry mechanism" in {

      val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
        testJourneyId,
        testInternalId,
        withCompanyData = true,
        Some(businessVerificationPassKey),
        Some(testCtutr)
      )

      val now: Instant = OffsetDateTime.now(ZoneOffset.UTC).toInstant

      val registrationTimestamp: Instant = Instant.ofEpochMilli(now.toEpochMilli - 500)

      val testJourneyDataWithRegistrationTimeout: JsObject = testJourneyData ++
        JourneyDataHelper.getRegistrationTimestamp(registrationTimestamp)

      insertById(testJourneyId, testInternalId, testJourneyDataWithRegistrationTimeout)

      stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

      val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

      delayedRegistration(testJourneyId, testInternalId, testSafeId, 1000)(ec)

      val result =  post("/register-limited-company", 2500.millis)(jsonBody)

      result.status mustBe OK
      result.json mustBe registrationSuccess

      retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

      verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")

    }

    "resubmit when registration does not complete in the timeout using retry mechanism" in {

      val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
        testJourneyId,
        testInternalId,
        withCompanyData = true,
        Some(businessVerificationPassKey),
        Some(testCtutr)
      )

      val now: Instant = OffsetDateTime.now(ZoneOffset.UTC).toInstant

      val registrationTimestamp: Instant = Instant.ofEpochMilli(now.toEpochMilli - 100)

      val testJourneyDataWithRegistrationTimeout: JsObject = testJourneyData ++
        JourneyDataHelper.getRegistrationTimestamp(registrationTimestamp)

      insertById(testJourneyId, testInternalId, testJourneyDataWithRegistrationTimeout)

      stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
      stubRegisterWithMultipleIdentifiersSuccess(testRegisterRegisteredSocietyJsonBody, testRegime)(OK, testSafeId)

      val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

      val result =  post("/register-registered-society", 2500.millis)(jsonBody)

      result.status mustBe OK
      result.json mustBe registrationSuccess

      retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

      verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")

    }


    "submit a registration after previously failing matching" when {

      "there is a registration status of registration not called, but no timestamp" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationPassKey),
          Some(testCtutr)
        )

        val testJourneyDataWithRegistrationStatus: JsObject = testJourneyData ++ JourneyDataHelper.getRegistrationNotCalledStatus

        insertById(testJourneyId, testInternalId, testJourneyDataWithRegistrationStatus)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubRegisterWithMultipleIdentifiersSuccess(testRegisterRegisteredSocietyJsonBody, testRegime)(OK, testSafeId)

        val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-registered-society")(jsonBody)

        result.status mustBe OK
        result.json mustBe registrationSuccess

        retrieveRegistrationStatus(testJourneyId, testInternalId) mustBe Some(Registered(testSafeId))

        verifyPost(1, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

    }

    "return INTERNAL_SERVER_ERROR" when {

      "the journey data is in an illegal state with a successful registration but no timestamp" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationPassKey),
          Some(testCtutr)
        )

        val testJourneyDataWithRegistrationStatus: JsObject = testJourneyData ++ JourneyDataHelper.getSuccessfulRegistrationStatus(testSafeId)

        insertById(testJourneyId, testInternalId, testJourneyDataWithRegistrationStatus)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe INTERNAL_SERVER_ERROR
        result.body mustBe "[VER-5038] Registration status is defined as success, but registration timeout is not defined"

        verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
      }

      "the journey data is in an illegal state with a failed registration but no timestamp" in {

        val testJourneyData: JsObject = JourneyDataHelper.getJourneyDataForRegistration(
          testJourneyId,
          testInternalId,
          withCompanyData = true,
          Some(businessVerificationPassKey),
          Some(testCtutr)
        )

        val testJourneyDataWithRegistrationStatus: JsObject = testJourneyData ++ JourneyDataHelper.getFailedRegistrationStatus(testInvalidPayloadCode, testInvalidPayloadReason)

        insertById(testJourneyId, testInternalId, testJourneyDataWithRegistrationStatus)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val jsonBody: JsObject = createJsonBody(testJourneyId, businessVerificationCheck = true, testRegime)

        val result = post("/register-limited-company")(jsonBody)

        result.status mustBe INTERNAL_SERVER_ERROR
        result.body mustBe "[VER-5038] Registration status is defined as failed, but registration timeout is not defined"

        verifyPost(0, s"""/cross-regime/register/GRS?grsRegime=$testRegime""")
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

  private def delayedRegistration(journeyId: String, authInternalId: String, safeId: String, delay: Long)(implicit ec: ExecutionContext): Future[Unit] = Future {

    Thread.sleep(delay)

    updateById(journeyId, authInternalId, "registration", Json.obj(
      "registrationStatus" -> "REGISTERED",
      "registeredBusinessPartnerId" -> safeId
      )
    )

  }

}
