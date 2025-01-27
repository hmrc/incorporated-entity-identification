/*
 * Copyright 2025 HM Revenue & Customs
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

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import play.api.test.Helpers._
import assets.TestConstants.{testInternalId, testInvalidPayloadCode, testInvalidPayloadReason, testInvalidRegimeCode, testInvalidRegimePayload, testJourneyId, testSafeId}
import uk.gov.hmrc.incorporatedentityidentification.models.{Failure, Registered, RegistrationFailed, RegistrationStatus}
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository
import uk.gov.hmrc.incorporatedentityidentification.services.{JourneyDataService, JourneyIdGenerationService}
import utils.ComponentSpecHelper

import scala.concurrent.ExecutionContext.global

class JourneyDataServiceISpec extends ComponentSpecHelper {

  val journeyRepo: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]
  val idGenerator: JourneyIdGenerationService = app.injector.instanceOf[JourneyIdGenerationService]

  val journeyDataService: JourneyDataService = new JourneyDataService(journeyRepo, idGenerator)(global)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(journeyRepo.drop)
  }

  "JourneyDataService" should {

    "be able to create and read a registration timestamp" in {

      await(journeyRepo.createJourney(testJourneyId, testInternalId))

      val now: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

      val timestamp: Instant = Instant.ofEpochMilli(now.toInstant.toEpochMilli)

      await(journeyDataService.updateRegistrationTimestamp(testJourneyId, testInternalId, timestamp))

      await(journeyDataService.retrieveRegistrationTimestamp(testJourneyId, testInternalId)) match {
        case Some(retrievedTimestamp) => retrievedTimestamp mustBe timestamp
        case None => fail("Error : The timestamp was not retrieved")
      }

    }

    "return a value of None for the registration timestamp when it is not present in the journey data" in {

      await(journeyRepo.createJourney(testJourneyId, testInternalId))

      await(journeyDataService.retrieveRegistrationTimestamp(testJourneyId, testInternalId)) mustBe None
    }

    "be able to save and read a registration status of Registered" in {

      await(journeyRepo.createJourney(testJourneyId, testInternalId))

      val registrationStatus: RegistrationStatus = Registered(testSafeId)

      await(journeyDataService.updateRegistrationStatus(testJourneyId, testInternalId, registrationStatus))

      await(journeyDataService.retrieveRegistrationStatus(testJourneyId, testInternalId)) match {
        case Some(retrievedRegistrationStatus) => retrievedRegistrationStatus match {
          case Registered(safeId) => safeId mustBe testSafeId
          case _ => fail("Error : Incorrect registration status retrieved")
        }
        case None => fail("Error : The registration status was not retrieved")
      }

    }

    "be able to save and read a registration status of RegistrationFailed with a single failure" in {

      await(journeyRepo.createJourney(testJourneyId, testInternalId))

      val failures: Array[Failure] = Array(Failure(testInvalidPayloadCode, testInvalidPayloadReason))

      val registrationStatus: RegistrationStatus = RegistrationFailed(Some(failures))

      await(journeyDataService.updateRegistrationStatus(testJourneyId, testInternalId, registrationStatus))

      await(journeyDataService.retrieveRegistrationStatus(testJourneyId, testInternalId)) match {
        case Some(retrievedRegistrationStatus) => retrievedRegistrationStatus match {
          case RegistrationFailed(optFailures) => optFailures match {
            case Some(retrievedFailures) => retrievedFailures mustBe failures
            case None => fail("Error : Single failure not found")
          }
          case _ => fail("Error : Incorrect registration status retrieved")
        }
        case None => fail("The registration status was not retrieved")
      }
    }

    "be able to save and read a registration status of RegistrationFailed with multiple failures" in {

      await(journeyRepo.createJourney(testJourneyId, testInternalId))

      val failures: Array[Failure] = Array(
        Failure(testInvalidPayloadCode, testInvalidPayloadReason),
        Failure(testInvalidRegimeCode, testInvalidRegimePayload)
      )

      val registrationStatus: RegistrationStatus = RegistrationFailed(Some(failures))

      await(journeyDataService.updateRegistrationStatus(testJourneyId, testInternalId, registrationStatus))

      await(journeyDataService.retrieveRegistrationStatus(testJourneyId, testInternalId)) match {
        case Some(retrievedRegistrationStatus) => retrievedRegistrationStatus match {
          case RegistrationFailed(optFailures) => optFailures match {
            case Some(retrievedFailures) => retrievedFailures mustBe failures
            case None => fail("Error : Multiple failures not found")
          }
          case _ => fail("Error : Incorrect registration status retrieved")
        }
        case None => fail("The registration status was not retrieved")
      }
    }

    "return a value of None when a registration status is not present in the journey data" in {

      await(journeyRepo.createJourney(testJourneyId, testInternalId))

      await(journeyDataService.retrieveRegistrationStatus(testJourneyId, testInternalId)) mustBe None
    }

  }

}
