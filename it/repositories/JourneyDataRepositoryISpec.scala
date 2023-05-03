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

package repositories

import java.util.UUID

import assets.TestConstants.{testInternalId, testJourneyId}
import play.api.libs.json.{JsString, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository._
import utils.ComponentSpecHelper

class JourneyDataRepositoryISpec extends ComponentSpecHelper {

  val repo: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.drop)
  }

  val testKey = "testKey"
  val testData = "test"

  "createJourney" should {
    "successfully insert the journeyId" in {
      await(repo.createJourney(testJourneyId, testInternalId)) mustBe testJourneyId
    }
  }
  s"getJourneyData($testJourneyId)" should {
    "successfully return all data" in {
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(_.-(CreationTimestampKey)) mustBe
        Some(Json.obj(JourneyIdKey -> testJourneyId, AuthInternalIdKey -> testInternalId))
    }
  }
  "updateJourneyData" should {
    "successfully insert data" in {
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(json => (json \ testKey).as[String]) mustBe Some(testData)
    }
    "successfully update data when data is already stored against a key" in {
      val updatedData = "updated"
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(json => (json \ testKey).as[String]) mustBe Some(testData)
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(updatedData), testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(json => (json \ testKey).as[String]) mustBe Some(updatedData)
    }
  }
  "removeJourneyDataField" should {
    "successfully remove a field" in {
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(json => (json \ testKey).as[String]) mustBe Some(testData)
      await(repo.removeJourneyDataField(testJourneyId, testInternalId, testKey)) mustBe true
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(_.-(CreationTimestampKey)) mustBe
        Some(Json.obj(JourneyIdKey -> testJourneyId, AuthInternalIdKey -> testInternalId))

    }
    "pass successfully when the field is not present" in {
      val testSecondKey = "secondKey"

      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.removeJourneyDataField(testJourneyId, testInternalId, testSecondKey)) mustBe true
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(_.-(CreationTimestampKey)) mustBe
        Some(Json.obj(JourneyIdKey -> testJourneyId, AuthInternalIdKey -> testInternalId, testKey -> testData))
    }
  }
  "removeJourneyData" should {

    "successfully remove data associated with journeyId" in {
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.removeJourneyData(testJourneyId, testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(_.-(CreationTimestampKey)) mustBe
        Some(Json.obj(JourneyIdKey -> testJourneyId, AuthInternalIdKey -> testInternalId))
    }

    "return false when an incorrect journey id is used" in {

      val newJourneyId: String = UUID.randomUUID().toString

      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))

      await(repo.removeJourneyData(newJourneyId, testInternalId)) mustBe false
    }
  }

}
