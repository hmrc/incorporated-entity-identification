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

package repositories

import assets.TestConstants.{testInternalId, testJourneyId}
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.Helpers._
import reactivemongo.play.json.collection.Helpers.idWrites
import uk.gov.hmrc.incorporatedentityidentification.models.IncorporatedEntityIdentificationModel
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository._
import utils.ComponentSpecHelper

import scala.concurrent.ExecutionContext.Implicits.global

class JourneyDataRepositoryISpec extends ComponentSpecHelper {

  val repo: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.drop)
  }

  val authInternalIdKey: String = "authInternalId"
  val creationTimestampKey: String = "creationTimestamp"

  "addedCreationTimestampIfMissing" should {
    def removeCreationTimestamp(): JsObject = Json.obj("$unset" -> Json.obj("creationTimestamp" -> ""))

    def setANewField(fieldName: String): JsObject = Json.obj("$set" -> Json.obj(fieldName -> "25"))

    def entryWithJourneyId(journeyId: Int): JsObject = Json.obj(
      journeyIdKey -> journeyId.toString,
      authInternalIdKey -> (journeyId * 10).toString
    )

    "do its job of adding creationTimestamp if missing" in {
      val actualDocumentsWithoutCreationTimestamp = for {
        _ <- repo.createJourney(journeyId = "1", authInternalId = "10")
        _ <- repo.createJourney(journeyId = "2", authInternalId = "20")
        _ <- repo.createJourney(journeyId = "3", authInternalId = "30")
        _ <- repo.createJourney(journeyId = "4", authInternalId = "40")

        _ <- repo.collection.update(true).one(entryWithJourneyId(journeyId = 1), removeCreationTimestamp())
        _ <- repo.collection.update(true).one(entryWithJourneyId(journeyId = 1), setANewField(fieldName = "name1"))

        _ <- repo.collection.update(true).one(entryWithJourneyId(journeyId = 3), removeCreationTimestamp())
        _ <- repo.collection.update(true).one(entryWithJourneyId(journeyId = 3), setANewField(fieldName = "name3"))

        _ <- repo.collection.update(true).one(entryWithJourneyId(journeyId = 4), removeCreationTimestamp())
        _ <- repo.collection.update(true).one(entryWithJourneyId(journeyId = 4), setANewField(fieldName = "name4"))

        documentsWithoutCreationTimestamp <- repo.count(Json.obj("creationTimestamp" -> Json.obj("$exists" -> false)))
      } yield
        documentsWithoutCreationTimestamp

      await(actualDocumentsWithoutCreationTimestamp) must be(3)

      await(repo.addCreationTimestampFieldIfMissing()).toString must be("MultiBulkWriteResult(true,3,3,List(),List(),None,None,None,3)")

      val journey1 = await(repo.getJourneyData("1", "10")).get
      journey1.keys must contain("creationTimestamp")
      journey1.keys must contain("name1")

      val journey2 = await(repo.getJourneyData("2", "20")).get
      journey2.keys must contain("creationTimestamp")

      val journey3 = await(repo.getJourneyData("3", "30")).get
      journey3.keys must contain("creationTimestamp")
      journey3.keys must contain("name3")

      val journey4 = await(repo.getJourneyData("4", "40")).get
      journey4.keys must contain("creationTimestamp")
      journey4.keys must contain("name4")

      await(repo.count(Json.obj("creationTimestamp" -> Json.obj("$exists" -> false)))) must be(0)

      await(repo.addCreationTimestampFieldIfMissing()).toString must be("MultiBulkWriteResult(true,0,0,List(),List(),None,None,None,0)")

    }
  }

  "createJourney" should {
    "successfully insert the journeyId" in {
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.findById(testJourneyId)) mustBe Some(IncorporatedEntityIdentificationModel(testJourneyId))
    }
  }
  s"getJourneyData($testJourneyId)" should {
    "successfully return all data" in {
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(_.-(creationTimestampKey)) mustBe Some(Json.obj(authInternalIdKey -> testInternalId))
    }
  }
  "updateJourneyData" should {
    "successfully insert data" in {
      val testKey = "testKey"
      val testData = "test"
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(json => (json \ testKey).as[String]) mustBe Some(testData)
    }
    "successfully update data when data is already stored against a key" in {
      val testKey = "testKey"
      val testData = "test"
      val updatedData = "updated"
      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(updatedData), testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(json => (json \ testKey).as[String]) mustBe Some(updatedData)
    }
  }
  "removeJourneyDataField" should {
    "successfully remove a field" in {
      val testKey = "testKey"
      val testData = "test"

      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.removeJourneyDataField(testJourneyId, testInternalId, testKey))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(_.-(creationTimestampKey)) mustBe Some(Json.obj(authInternalIdKey -> testInternalId))

    }
    "pass successfully when the field is not present" in {
      val testKey = "testKey"
      val testData = "test"
      val testSecondKey = "secondKey"

      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.removeJourneyDataField(testJourneyId, testInternalId, testSecondKey))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(_.-(creationTimestampKey)) mustBe
        Some(Json.obj(authInternalIdKey -> testInternalId, testKey -> testData))
    }
  }
  "removeJourneyData" should {
    "successfully remove data associated with journeyId" in {
      val testKey = "testKey"
      val testData = "test"

      await(repo.createJourney(testJourneyId, testInternalId))
      await(repo.updateJourneyData(testJourneyId, testKey, JsString(testData), testInternalId))
      await(repo.removeJourneyData(testJourneyId, testInternalId))
      await(repo.getJourneyData(testJourneyId, testInternalId)).map(_.-(creationTimestampKey)) mustBe Some(Json.obj(authInternalIdKey -> testInternalId))
    }
  }

}

