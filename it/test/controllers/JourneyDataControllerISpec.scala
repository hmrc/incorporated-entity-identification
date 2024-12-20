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
import play.api.Application
import play.api.http.Status.CREATED
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}
import play.api.test.Helpers._
import stubs.{AuthStub, FakeJourneyIdGenerationService}
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository._
import uk.gov.hmrc.incorporatedentityidentification.services.JourneyIdGenerationService
import utils.{ComponentSpecHelper, CustomMatchers, JourneyDataMongoHelper}

class JourneyDataControllerISpec extends ComponentSpecHelper with CustomMatchers with JourneyDataMongoHelper with AuthStub {
  lazy val testJourneyId = "testJourneyId"
  lazy val testIncorrectAuthInternalId = "testIncorrectAuthInternalId"
  lazy val testDataKey = "testDataKey"
  lazy val testDataValue = "testDataValue"

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[JourneyIdGenerationService].toInstance(new FakeJourneyIdGenerationService(testJourneyId)))
    .configure(config)
    .build()

  "POST /journey " when {
    "a new journey begins" should {
      "return OK with the newly generated journey ID" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val res = post("/journey")(Json.obj())

        res.status mustBe CREATED
        (res.json \ "journeyId").as[String] mustBe testJourneyId
        findById(testJourneyId, testInternalId).map(_.-(CreationTimestampKey)) mustBe
          Some(Json.obj(JourneyIdKey -> testJourneyId, AuthInternalIdKey -> testInternalId))
      }
      "return Unauthorised for authentication failure" in {
        stubAuthFailure()
        val res = post("/journey")(Json.obj())

        res.status mustBe UNAUTHORIZED
      }
      "return Unauthorised when internalId is not defined" in {
        stubAuth(OK, emptyAuthResponse())

        val res = post("/journey")(Json.obj())
        res.status mustBe UNAUTHORIZED
      }
    }
  }

  "GET /journey/:journeyId" when {
    "there is data stored against the journey ID" should {
      "return all data stored against the journey ID" in {
        val testData = Json.obj(
          testDataKey -> testDataValue
        )

        insertById(testJourneyId, testInternalId, testData)

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val expectedData = Json.obj(
          JourneyIdKey      -> testJourneyId,
          AuthInternalIdKey -> testInternalId,
          testDataKey       -> testDataValue
        )

        val res = get(s"/journey/$testJourneyId")

        res.status mustBe OK
        res.json mustBe expectedData
      }
    }
    "there is no data stored against the journey ID" should {
      "return NOT_FOUND with a code explaining that no data can be found" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val res = get(s"/journey/$testJourneyId")

        res.status mustBe NOT_FOUND
        res.json mustBe Json.obj(
          "code"   -> "NOT_FOUND",
          "reason" -> s"No data exists for journey ID: $testJourneyId"
        )
      }
    }
    "the user cannot be authorised" should {
      "return Unauthorised" in {
        stubAuthFailure()

        val res = get(s"/journey/$testJourneyId")

        res.status mustBe UNAUTHORIZED
      }
    }
    "the provided internal ID does not match the ID on the record" should {
      "return Not Found" in {
        stubAuth(OK, successfulAuthResponse(Some(testIncorrectAuthInternalId)))

        val testData = Json.obj(
          testDataKey -> testDataValue
        )
        insertById(testJourneyId, testInternalId, testData)

        val res = get(s"/journey/$testJourneyId")

        res.status mustBe NOT_FOUND
      }
    }
    "the response from auth does not include the internal id" should {
      "return unauthorised" in {
        stubAuth(OK, emptyAuthResponse())

        val res = get(s"/journey/$testJourneyId")
        res.status mustBe UNAUTHORIZED
      }
    }
  }

  "GET /journey/:journeyId/:dataKey" when {
    "there is data stored against the journey ID containing the value in dataKey" should {
      "return all the data stored against the journeyId and dataKey" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val testData = Json.obj(
          testDataKey -> testDataValue
        )
        insertById(testJourneyId, testInternalId, testData)

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe OK
        res.json mustBe JsString(testDataValue)
      }
    }

    "there is data stored against the journey ID but no data for the dataKey" should {
      "return NOT FOUND with a code indicating there is no data" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        insertById(testJourneyId, testInternalId)

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe NOT_FOUND
        res.json mustBe Json.obj(
          "code"   -> "NOT_FOUND",
          "reason" -> s"No data exists for either journey ID: $testJourneyId or data key: $testDataKey"
        )
      }
    }

    "there is no data stored against the journey ID" should {
      "return all the data stored against the journeyId and dataKey" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        insertById(testJourneyId, testInternalId)

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe NOT_FOUND
        res.json mustBe Json.obj(
          "code"   -> "NOT_FOUND",
          "reason" -> s"No data exists for either journey ID: $testJourneyId or data key: $testDataKey"
        )
      }
    }

    "the user cannot be authorised" should {
      "return Unauthorised" in {
        stubAuthFailure()

        val testDataKey = "testDataKey"

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe UNAUTHORIZED
      }
    }
    "the provided internal ID does not match the ID on the record" should {
      "return Not Found" in {
        stubAuth(OK, successfulAuthResponse(Some(testIncorrectAuthInternalId)))

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe NOT_FOUND
      }
    }
    "the response from auth does not include the internal id" should {
      "return unauthorised" in {
        stubAuth(OK, emptyAuthResponse())

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe UNAUTHORIZED
      }
    }

  }

  "PUT /journey/:journeyId/:dataKey" when {
    "there is a journey for the provided journey ID" should {
      "update the data with the provided data" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        insertById(testJourneyId, testInternalId)

        val res = put(s"/journey/$testJourneyId/$testDataKey")(testDataValue)

        res.status mustBe OK

        findById(testJourneyId, testInternalId) mustBe Some(
          Json.obj(
            JourneyIdKey      -> testJourneyId,
            AuthInternalIdKey -> testInternalId,
            testDataKey       -> testDataValue
          )
        )
      }
    }
    "there is no journey for the provided journey ID" should {
      "return Internal Server Error" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val res = put(s"/journey/$testJourneyId/$testDataKey")(testDataValue)

        res.status mustBe INTERNAL_SERVER_ERROR

        findById(testJourneyId, testInternalId) mustBe None
      }
    }
    "the user cannot be authorised" should {
      "return Unauthorised" in {
        stubAuthFailure()

        val res = put(s"/journey/$testJourneyId/$testDataKey")(testDataValue)

        res.status mustBe UNAUTHORIZED
      }
    }
    "the provided internal ID does not match the ID on the record" should {
      "return Internal Server Error" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        insertById(testJourneyId, testIncorrectAuthInternalId)

        val res = put(s"/journey/$testJourneyId/$testDataKey")(testDataValue)

        res.status mustBe INTERNAL_SERVER_ERROR
      }
    }
    "the response from auth does not include the internal id" should {
      "return unauthorized" in {
        stubAuth(OK, emptyAuthResponse())

        val res = put(s"/journey/$testJourneyId/$testDataKey")(testDataValue)

        res.status mustBe UNAUTHORIZED
      }
    }
  }

  "DELETE /journey/:journeyId/:dataKey" when {
    "there is a journey for the provided journey ID" should {
      "remove the data with the provided data key" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        insertById(testJourneyId, testInternalId, Json.obj(testDataKey -> testDataValue))

        val res = delete(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe NO_CONTENT

        findById(testJourneyId, testInternalId) mustBe Some(
          Json.obj(
            JourneyIdKey      -> testJourneyId,
            AuthInternalIdKey -> testInternalId
          )
        )
      }
      "return NO_CONTENT if the data field does not exist" in {
        val alternativeTestDataKey: String = "testAlternativeDataKey"

        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        insertById(testJourneyId, testInternalId, Json.obj(testDataKey -> testDataValue))

        val res = delete(s"/journey/$testJourneyId/$alternativeTestDataKey")

        res.status mustBe NO_CONTENT

        findById(testJourneyId, testInternalId) mustBe Some(
          Json.obj(
            JourneyIdKey      -> testJourneyId,
            AuthInternalIdKey -> testInternalId,
            testDataKey       -> testDataValue
          )
        )
      }
    }
    "there is no journey for the provided journey ID" should {
      "return Internal Server Error" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val res = delete(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe INTERNAL_SERVER_ERROR

        findById(testJourneyId, testInternalId) mustBe None
      }
    }
    "the user cannot be authorised" should {
      "return Unauthorised" in {
        stubAuthFailure()

        val testDataKey = "testDataKey"

        val res = delete(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe UNAUTHORIZED
      }
    }
    "the response from auth does not contain an internal id" should {
      "return Unauthorized" in {
        stubAuth(OK, emptyAuthResponse())

        val testDataKey = "testDataKey"

        val res = delete(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe UNAUTHORIZED
      }
    }
  }

  "DELETE /journey/:journeyId" when {
    "there is a journey for the provided journey ID" should {
      "remove the data from the record" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        insertById(testJourneyId, testInternalId, Json.obj(testDataKey -> testDataValue))

        val res = delete(s"/journey/$testJourneyId")

        res.status mustBe NO_CONTENT

        findById(testJourneyId, testInternalId).map(_.-(CreationTimestampKey)) mustBe Some(
          Json.obj(
            JourneyIdKey      -> testJourneyId,
            AuthInternalIdKey -> testInternalId
          )
        )

        findById(testJourneyId, testInternalId).map(_.keys.contains(CreationTimestampKey)) mustBe Some(true)
      }
    }
    "there is no journey for the provided journey ID" should {
      "return Internal Server Error" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        val res = delete(s"/journey/$testJourneyId")

        res.status mustBe INTERNAL_SERVER_ERROR

        findById(testJourneyId, testInternalId) mustBe None
      }
    }
    "the user cannot be authorised" should {
      "return Unauthorised" in {
        stubAuthFailure()

        val res = delete(s"/journey/$testJourneyId")

        res.status mustBe UNAUTHORIZED
      }
    }
    "the response from auth does not include an internal id" should {
      "return Unauthorized" in {
        stubAuth(OK, emptyAuthResponse())

        val res = delete(s"/journey/$testJourneyId")

        res.status mustBe UNAUTHORIZED
      }
    }
  }
}
