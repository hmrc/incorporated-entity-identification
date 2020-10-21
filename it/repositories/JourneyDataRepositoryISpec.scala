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

package repositories

import assets.TestConstants.{testInternalId, testJourneyId}
import play.api.libs.json.{JsString, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.incorporatedentityidentification.models.IncorporatedEntityIdentificationModel
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository
import utils.ComponentSpecHelper

import scala.concurrent.ExecutionContext.Implicits.global

class JourneyDataRepositoryISpec extends ComponentSpecHelper {

  val repo: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]

  override def beforeEach: Unit = {
    super.beforeEach()
    await(repo.drop)
  }

  val authInternalIdKey: String = "authInternalId"
  val creationTimestampKey: String = "creationTimestamp"

  "createJourney" should {
    "successfully insert the journeyId" in {
      repo.createJourney(testJourneyId, None)
      await(repo.findById(testJourneyId)) mustBe Some(IncorporatedEntityIdentificationModel(testJourneyId))
    }
  }
  s"getJourneyData($testJourneyId)" should {
    "successfully return all data" in {
      await(repo.createJourney(testJourneyId, Some(testInternalId)))
      await(repo.getJourneyData(testJourneyId)).map(_.-(creationTimestampKey)) mustBe Some(Json.obj(authInternalIdKey -> testInternalId))
    }
  }
  "updateJourneyData" should {
    "successfully insert data" in {
      val testKey = "testKey"
      val testData = "test"
      await(repo.createJourney(testJourneyId, Some(testInternalId)))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData)))
      await(repo.getJourneyData(testJourneyId, testKey)) mustBe Some(Json.obj(testKey -> testData))
    }
    "successfully update data when data is already stored against a key" in {
      val testKey = "testKey"
      val testData = "test"
      val updateData = "updated"
      await(repo.createJourney(testJourneyId, Some(testInternalId)))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData)))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(updateData)))
      await(repo.getJourneyData(testJourneyId, testKey)) mustBe Some(Json.obj(testKey -> updateData))
    }

  }

}

