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

package services

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository
import uk.gov.hmrc.incorporatedentityidentification.services.{JourneyDataService, JourneyIdGenerationService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneyDataServiceSpec extends AnyWordSpec with Matchers with IdiomaticMockito with ResetMocksAfterEachTest {
  val mockJourneyDataRepository: JourneyDataRepository = mock[JourneyDataRepository]
  val mockJourneyIdGenerationService: JourneyIdGenerationService = mock[JourneyIdGenerationService]

  object TestJourneyDataService extends JourneyDataService(mockJourneyDataRepository, mockJourneyIdGenerationService)

  val testJourneyId = "testJourneyId"
  val testInternalId = "testInternalId"

  "createJourney" should {
    "call to store a new journey with the generated journey ID" in {
      mockJourneyIdGenerationService.generateJourneyId() returns testJourneyId
      mockJourneyDataRepository.createJourney(eqTo(testJourneyId), eqTo(testInternalId)) returns Future.successful(testJourneyId)

      await(TestJourneyDataService.createJourney(testInternalId)) mustBe testJourneyId
    }
  }

  "getJourneyData" should {
    "return the stored journey data" when {
      "the data exists in the database" in {
        val testJourneyData = Json.obj("testKey" -> "testValue")

        mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(Some(testJourneyData))

        await(TestJourneyDataService.getJourneyData(testJourneyId, testInternalId)) mustBe Some(testJourneyData)
      }
    }
    "return None" when {
      "the data does not exist in the database" in {
        mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(None)

        await(TestJourneyDataService.getJourneyData(testJourneyId, testInternalId)) mustBe None
      }
    }
  }

  "getJourneyDataByKey" should {
    "return the stored journey data for the key provided" when {
      "the data exists in the database" in {
        val testKey = "testKey"
        val testValue = "testValue"

        val testJourneyData = Json.obj(testKey -> testValue)

        mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(Some(testJourneyData))

        await(TestJourneyDataService.getJourneyDataByKey(testJourneyId, testKey, testInternalId)) mustBe Some(JsString(testValue))

      }
    }
    "return None" when {
      "the data does not exist in the database" in {
        val testKey = "testKey"

        mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(None)

        await(TestJourneyDataService.getJourneyDataByKey(testJourneyId, testKey, testInternalId)) mustBe None
      }
    }
  }

  "updateJourneyData" should {

    val testKey = "testKey"
    val testValue = JsString("testValue")

    "return true when the data field is successfully updated" in {

      mockJourneyDataRepository.updateJourneyData(testJourneyId, testKey, testValue, testInternalId) returns Future.successful(true)

      await(TestJourneyDataService.updateJourneyData(testJourneyId, testKey, testValue, testInternalId)) mustBe true
    }

    "return false when an update fails" in {

      mockJourneyDataRepository.updateJourneyData(testJourneyId, testKey, testValue, testInternalId) returns Future.successful(false)

      await(TestJourneyDataService.updateJourneyData(testJourneyId, testKey, testValue, testInternalId)) mustBe false
    }
  }

  "removeJourneyDataField" should {

    val testKey = "testKey"

    "return true when the data field is successfully removed" in {

      mockJourneyDataRepository.removeJourneyDataField(testJourneyId, testInternalId, testKey) returns Future.successful(true)

      await(TestJourneyDataService.removeJourneyDataField(testJourneyId, testInternalId, testKey)) mustBe true
    }

    "return false if the data field is not successfully removed" in {

      mockJourneyDataRepository.removeJourneyDataField(testJourneyId, testInternalId, testKey) returns Future.successful(false)

      await(TestJourneyDataService.removeJourneyDataField(testJourneyId, testInternalId, testKey)) mustBe false
    }
  }

  "removeJourneyData" should {

    "return true if the journey data is successfully removed" in {

      mockJourneyDataRepository.removeJourneyData(testJourneyId, testInternalId) returns Future.successful(true)

      await(TestJourneyDataService.removeJourneyData(testJourneyId, testInternalId)) mustBe true
    }
  }

}
