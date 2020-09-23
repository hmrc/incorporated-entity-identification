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

import assets.TestConstants._
import play.api.Application
import play.api.http.Status.CREATED
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}
import play.api.test.Helpers._
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import stubs.{AuthStub, FakeJourneyIdGenerationService}
import uk.gov.hmrc.incorporatedentityidentification.services.JourneyIdGenerationService
import utils.{ComponentSpecHelper, CustomMatchers, JourneyDataMongoHelper}

class JourneyDataControllerISpec extends ComponentSpecHelper with CustomMatchers with JourneyDataMongoHelper with AuthStub {
  lazy val testJourneyId = "testJourneyId"

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
        findById(testJourneyId) mustBe Some(Json.obj("_id" -> testJourneyId, "authInternalId" -> testInternalId))
      }
      "return Unauthorised" in {
        stubAuthFailure()
        val res = post("/journey")(Json.obj())

        res.status mustBe UNAUTHORIZED
      }
    }
  }

  "GET /journey/:journeyId" when {
    "there is data stored against the journey ID" should {
      "return all data stored against the journey ID" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val testData = Json.obj(
          "testField" -> "testValue"
        )
        insertById(testJourneyId, testData)

        val res = get(s"/journey/$testJourneyId")

        res.status mustBe OK
        res.json mustBe testData
      }
    }
    "there is no data stored against the journey ID" should {
      "return NOT_FOUND with a code explaining that no data can be found" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val res = get(s"/journey/$testJourneyId")

        res.status mustBe NOT_FOUND
        res.json mustBe Json.obj(
          "code" -> "NOT_FOUND",
          "reason" -> s"No data exists for journey ID: $testJourneyId"
        )
      }
    }
    "the auth internalIds do not match" should {
      "return Unauthorised" in {
        stubAuthFailure()

        val testData = Json.obj(
          "testField" -> "testValue"
        )
        insertById(testJourneyId, testData)

        val res = get(s"/journey/$testJourneyId")

        res.status mustBe UNAUTHORIZED
      }
    }
  }

  "GET /journey/:journeyId/:dataKey" when {
    "there is data stored against the journey ID containing the value in dataKey" should {
      "return all the data stored against the journeyId and dataKey" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val testDataKey = "testDataKey"
        val testDataValue = "testDataValue"

        val testData = Json.obj(
          testDataKey -> testDataValue
        )
        insertById(testJourneyId, testData)

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe OK
        res.json mustBe JsString(testDataValue)
      }
      "return Unathorised" in {
        stubAuthFailure()

        val testDataKey = "testDataKey"
        val testDataValue = "testDataValue"

        val testData = Json.obj(
          testDataKey -> testDataValue
        )
        insertById(testJourneyId, testData)

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe UNAUTHORIZED
      }
    }

    "there is data stored against the journey ID but no data for the dataKey" should {
      "return NOT FOUND with a code indicating there is no data" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val testDataKey = "testDataKey"

        insertById(testJourneyId)

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe NOT_FOUND
        res.json mustBe Json.obj(
          "code" -> "NOT_FOUND",
          "reason" -> s"No data exists for either journey ID: $testJourneyId or data key: $testDataKey"
        )
      }
    }

    "there is no data stored against the journey ID" should {
      "return all the data stored against the journeyId and dataKey" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val testDataKey = "testDataKey"

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe NOT_FOUND
        res.json mustBe Json.obj(
          "code" -> "NOT_FOUND",
          "reason" -> s"No data exists for either journey ID: $testJourneyId or data key: $testDataKey"
        )
      }
    }

    "the auth internalIds do not match" should {
      "return Unauthorised" in {
        stubAuthFailure()

        val testDataKey = "testDataKey"
        val testDataValue = "testDataValue"

        val testData = Json.obj(
          testDataKey -> testDataValue
        )
        insertById(testJourneyId, testData)

        val res = get(s"/journey/$testJourneyId/$testDataKey")

        res.status mustBe UNAUTHORIZED
      }
    }

  }

  "PUT /journey/:journeyId/:dataKey" when {
    "there is a journey for the provided journey ID" should {
      "update the data with the provided data" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val testDataKey = "testDataKey"
        val testDataValue = "testDataValue"

        insertById(testJourneyId)

        val res = put(s"/journey/$testJourneyId/$testDataKey")(testDataValue)

        res.status mustBe OK

        findById(testJourneyId) mustBe Some(
          Json.obj(
            "_id" -> testJourneyId,
            testDataKey -> testDataValue
          )
        )
      }
    }
    "there is no journey for the provided journey ID" should {
      "fail" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        val testDataKey = "testDataKey"
        val testDataValue = "testDataValue"

        val res = put(s"/journey/$testJourneyId/$testDataKey")(testDataValue)

        res.status mustBe INTERNAL_SERVER_ERROR

        findById(testJourneyId) mustBe empty
      }
    }
    "the auth internalIds do not match" should {
      "return Unauthorised" in {
        stubAuthFailure()

        val testDataKey = "testDataKey"
        val testDataValue = "testDataValue"

        insertById(testJourneyId)

        val res = put(s"/journey/$testJourneyId/$testDataKey")(testDataValue)

        res.status mustBe UNAUTHORIZED
      }
    }
  }
}
