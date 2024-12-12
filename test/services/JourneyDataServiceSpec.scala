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
import org.scalatest.Succeeded
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.incorporatedentityidentification.models.{BusinessVerificationFail, BusinessVerificationPass, CompanyProfile}
import uk.gov.hmrc.incorporatedentityidentification.models.BusinessVerificationStatus._
import uk.gov.hmrc.incorporatedentityidentification.models.error.DataAccessException
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

  val testCompanyName: String = "Test Company Ltd"
  val testCompanyNumber: String = "01234567"
  val testDateOfIncorporation: String = "2020-01-01"

  val testAddressLine1: String = "testLine1"
  val testAddressLine2: String = "test town"
  val testCareOf: String = "test name"
  val testCountry: String = "United Kingdom"
  val testLocality: String = "test city"
  val testPoBox: String = "123"
  val testPostalCode: String = "AA11AA"
  val testPremises: String = "1"
  val testRegion: String = "test region"

  val testCtUtr: String = "1234567890"

  val testCompleteJourneyData: String =
    s"""{
      |"_id" : "$testJourneyId",
      |"authInternalId" : "$testInternalId",
      |"creationTimestamp" : "2024-12-05T11:09:11",
      |"companyProfile" : {
      |  "companyName" : "$testCompanyName",
      |  "companyNumber" : "$testCompanyNumber",
      |  "dateOfIncorporation" : "$testDateOfIncorporation",
      |  "unsanitisedCHROAddress" :
      |    { "address_line_1" : "$testAddressLine1",
      |      "address_line_2" : "$testAddressLine2",
      |      "care_of" : "$testCareOf",
      |      "country" : "$testCountry",
      |      "locality" : "$testLocality",
      |      "po_box" : "$testPoBox",
      |      "postal_code" : "$testPostalCode",
      |      "premises" : "$testPremises",
      |      "region" : "$testRegion"
      |    }
      |  },
      |"ctutr" : "$testCtUtr",
      | "identifiersMatch" : "DetailsMatched",
      | "businessVerification" : {
      |   "verificationStatus" : "$businessVerificationPassKey"
      | },
      |"registration" :
      |  { "registrationStatus" : "REGISTERED",
      |    "registeredBusinessPartnerId" : "X00000123456789"
      |  }
      |}""".stripMargin

  val testFailedBusinessVerificationCompleteJourneyData: String =
    s"""{
       |"_id" : "$testJourneyId",
       |"authInternalId" : "$testInternalId",
       |"creationTimestamp" : "2024-12-05T11:09:11",
       |"companyProfile" : {
       |  "companyName" : "$testCompanyName",
       |  "companyNumber" : "$testCompanyNumber",
       |  "dateOfIncorporation" : "$testDateOfIncorporation",
       |  "unsanitisedCHROAddress" :
       |    { "address_line_1" : "$testAddressLine1",
       |      "address_line_2" : "$testAddressLine2",
       |      "care_of" : "$testCareOf",
       |      "country" : "$testCountry",
       |      "locality" : "$testLocality",
       |      "po_box" : "$testPoBox",
       |      "postal_code" : "$testPostalCode",
       |      "premises" : "$testPremises",
       |      "region" : "$testRegion"
       |    }
       |  },
       |"ctutr" : "$testCtUtr",
       | "identifiersMatch" : "DetailsMatched",
       | "businessVerification" : {
       |   "verificationStatus" : "$businessVerificationFailKey"
       | },
       |"registration" :
       |  { "registrationStatus" : "REGISTERED",
       |    "registeredBusinessPartnerId" : "X00000123456789"
       |  }
       |}""".stripMargin

  val testInvalidBusinessVerificationStatusJourneyData: String =
    s"""{
       |"_id" : "$testJourneyId",
       |"authInternalId" : "$testInternalId",
       |"creationTimestamp" : "2024-12-05T11:09:11",
       |"companyProfile" : {
       |  "companyName" : "$testCompanyName",
       |  "companyNumber" : "$testCompanyNumber",
       |  "dateOfIncorporation" : "$testDateOfIncorporation",
       |  "unsanitisedCHROAddress" :
       |    { "address_line_1" : "$testAddressLine1",
       |      "address_line_2" : "$testAddressLine2",
       |      "care_of" : "$testCareOf",
       |      "country" : "$testCountry",
       |      "locality" : "$testLocality",
       |      "po_box" : "$testPoBox",
       |      "postal_code" : "$testPostalCode",
       |      "premises" : "$testPremises",
       |      "region" : "$testRegion"
       |    }
       |  },
       |"ctutr" : "$testCtUtr",
       | "identifiersMatch" : "DetailsMatched",
       | "businessVerification" : {
       |   "verificationStatus" : "Invalid"
       | },
       |"registration" :
       |  { "registrationStatus" : "REGISTERED",
       |    "registeredBusinessPartnerId" : "X00000123456789"
       |  }
       |}""".stripMargin

  val testInvalidCompanyProfileJourneyData: String =
    s"""{
       |"_id" : "$testJourneyId",
       |"authInternalId" : "$testInternalId",
       |"creationTimestamp" : "2024-12-05T11:09:11",
       |"companyProfile" : {},
       |"ctutr" : "$testCtUtr",
       | "identifiersMatch" : "DetailsMatched",
       | "businessVerification" : {
       |   "verificationStatus" : "$businessVerificationPassKey"
       | },
       |"registration" :
       |  { "registrationStatus" : "REGISTERED",
       |    "registeredBusinessPartnerId" : "X00000123456789"
       |  }
       |}""".stripMargin

  val testInvalidCtUtrJourneyData: String =
    s"""{
       |"_id" : "$testJourneyId",
       |"authInternalId" : "$testInternalId",
       |"creationTimestamp" : "2024-12-05T11:09:11",
       |"companyProfile" : {
       |  "companyName" : "$testCompanyName",
       |  "companyNumber" : "$testCompanyNumber",
       |  "dateOfIncorporation" : "$testDateOfIncorporation",
       |  "unsanitisedCHROAddress" :
       |    { "address_line_1" : "$testAddressLine1",
       |      "address_line_2" : "$testAddressLine2",
       |      "care_of" : "$testCareOf",
       |      "country" : "$testCountry",
       |      "locality" : "$testLocality",
       |      "po_box" : "$testPoBox",
       |      "postal_code" : "$testPostalCode",
       |      "premises" : "$testPremises",
       |      "region" : "$testRegion"
       |    }
       |  },
       |"ctutr" : 1000,
       | "identifiersMatch" : "DetailsMatched",
       | "businessVerification" : {
       |   "verificationStatus" : "$businessVerificationPassKey"
       | },
       |"registration" :
       |  { "registrationStatus" : "REGISTERED",
       |    "registeredBusinessPartnerId" : "X00000123456789"
       |  }
       |}""".stripMargin

  val testMissingBusinessVerificationStatusJourneyData: String =
    s"""{
       |"_id" : "$testJourneyId",
       |"authInternalId" : "$testInternalId",
       |"creationTimestamp" : "2024-12-05T11:09:11",
       |"companyProfile" : {
       |  "companyName" : "$testCompanyName",
       |  "companyNumber" : "$testCompanyNumber",
       |  "dateOfIncorporation" : "$testDateOfIncorporation",
       |  "unsanitisedCHROAddress" :
       |    { "address_line_1" : "$testAddressLine1",
       |      "address_line_2" : "$testAddressLine2",
       |      "care_of" : "$testCareOf",
       |      "country" : "$testCountry",
       |      "locality" : "$testLocality",
       |      "po_box" : "$testPoBox",
       |      "postal_code" : "$testPostalCode",
       |      "premises" : "$testPremises",
       |      "region" : "$testRegion"
       |    }
       |  },
       |"ctutr" : "$testCtUtr",
       | "identifiersMatch" : "DetailsMatched",
       |"registration" :
       |  { "registrationStatus" : "REGISTERED",
       |    "registeredBusinessPartnerId" : "X00000123456789"
       |  }
       |}""".stripMargin

  val testMissingCompanyProfileJourneyData: String =
    s"""{
       |"_id" : "$testJourneyId",
       |"authInternalId" : "$testInternalId",
       |"creationTimestamp" : "2024-12-05T11:09:11",
       |"ctutr" : "$testCtUtr",
       | "identifiersMatch" : "DetailsMatched",
       | "businessVerification" : {
       |   "verificationStatus" : "$businessVerificationPassKey"
       | },
       |"registration" :
       |  { "registrationStatus" : "REGISTERED",
       |    "registeredBusinessPartnerId" : "X00000123456789"
       |  }
       |}""".stripMargin

  val testMissingCtUtrJourneyData: String =
    s"""{
       |"_id" : "$testJourneyId",
       |"authInternalId" : "$testInternalId",
       |"creationTimestamp" : "2024-12-05T11:09:11",
       |"companyProfile" : {
       |  "companyName" : "$testCompanyName",
       |  "companyNumber" : "$testCompanyNumber",
       |  "dateOfIncorporation" : "$testDateOfIncorporation",
       |  "unsanitisedCHROAddress" :
       |    { "address_line_1" : "$testAddressLine1",
       |      "address_line_2" : "$testAddressLine2",
       |      "care_of" : "$testCareOf",
       |      "country" : "$testCountry",
       |      "locality" : "$testLocality",
       |      "po_box" : "$testPoBox",
       |      "postal_code" : "$testPostalCode",
       |      "premises" : "$testPremises",
       |      "region" : "$testRegion"
       |    }
       |  },
       | "identifiersMatch" : "DetailsMatched",
       | "businessVerification" : {
       |   "verificationStatus" : "PASS"
       | },
       |"registration" :
       |  { "registrationStatus" : "REGISTERED",
       |    "registeredBusinessPartnerId" : "X00000123456789"
       |  }
       |}""".stripMargin

  val testCompleteJourneyDataAsJsObject: JsObject = Json.parse(testCompleteJourneyData).as[JsObject]

  val testFailedBusinessVerificationCompleteJourneyDataAsJsObject: JsObject =
    Json.parse(testFailedBusinessVerificationCompleteJourneyData).as[JsObject]

  val testInvalidBusinessVerificationStatusJourneyDataAsJsObject: JsObject =
    Json.parse(testInvalidBusinessVerificationStatusJourneyData).as[JsObject]

  val testInvalidCompanyProfileJourneyDataAsJsObject: JsObject =
    Json.parse(testInvalidCompanyProfileJourneyData).as[JsObject]

  val testInvalidCtUtrJourneyDataAsJsObject: JsObject =
    Json.parse(testInvalidCtUtrJourneyData).as[JsObject]

  val testMissingBusinessVerificationStatusJourneyDataAsJsObject: JsObject =
    Json.parse(testMissingBusinessVerificationStatusJourneyData).as[JsObject]

  val testMissingCompanyProfileJourneyDataAsJsObject: JsObject =
    Json.parse(testMissingCompanyProfileJourneyData).as[JsObject]

  val testMissingCtUtrJourneyDataAsJsObject: JsObject = Json.parse(testMissingCtUtrJourneyData).as[JsObject]

  val expectedUnsanitisedCHROAddress: JsObject = Json.obj(
    "address_line_1" -> testAddressLine1,
    "address_line_2" -> testAddressLine2,
    "care_of"        -> testCareOf,
    "country"        -> testCountry,
    "locality"       -> testLocality,
    "po_box"         -> testPoBox,
    "postal_code"    -> testPostalCode,
    "premises"       -> testPremises,
    "region"         -> testRegion
  )

  val expectedCompanyProfile: CompanyProfile =
    CompanyProfile(testCompanyName, testCompanyNumber, Some(testDateOfIncorporation), expectedUnsanitisedCHROAddress)

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

  "retrieveBusinessVerificationStatus" should {

    "retrieve the business verification status from the journey data where the status is pass" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(Some(testCompleteJourneyDataAsJsObject))

      await(TestJourneyDataService.retrieveBusinessVerificationStatus(testJourneyId, testInternalId)) mustBe Some(BusinessVerificationPass)
    }

    "retrieve the business verification status from the journey data where the status is fail" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(
        Some(testFailedBusinessVerificationCompleteJourneyDataAsJsObject)
      )

      await(TestJourneyDataService.retrieveBusinessVerificationStatus(testJourneyId, testInternalId)) mustBe Some(BusinessVerificationFail)
    }

    "raise a DataAccessException if there is an error parsing the business verification status" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(
        Some(testInvalidBusinessVerificationStatusJourneyDataAsJsObject)
      )

      await(
        TestJourneyDataService.retrieveBusinessVerificationStatus(testJourneyId, testInternalId).failed.map { ex =>
          ex mustBe a[DataAccessException]
          ex.getMessage mustBe s"Error occurred parsing business verification status for journey $testJourneyId"
        }
      ) mustBe Succeeded

    }

    "return none if the journey data does not contain the business verification status" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(
        Some(testMissingBusinessVerificationStatusJourneyDataAsJsObject)
      )

      await(TestJourneyDataService.retrieveBusinessVerificationStatus(testJourneyId, testInternalId)) mustBe None
    }

    "return none if the journey data is not found" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(None)

      await(TestJourneyDataService.retrieveBusinessVerificationStatus(testJourneyId, testInternalId)) mustBe None
    }
  }

  "retrieveCompanyProfileAndCtUtr" should {

    "return a company profile and Ct Utr when the journey data is fully defined" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(Some(testCompleteJourneyDataAsJsObject))

      val result: (Option[CompanyProfile], Option[String]) =
        await(TestJourneyDataService.retrieveCompanyProfileAndCtUtr(testJourneyId, testInternalId))

      result match {
        case (Some(companyProfile: CompanyProfile), Some(ctUtr: String)) =>
          companyProfile mustBe expectedCompanyProfile
          ctUtr mustBe testCtUtr
        case _ => fail("Call to retrieveCompanyProfileAndCtUtr should return company profile and Ct Utr")
      }

    }

    "raise a DataAccessException if there is an error parsing the company profile" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(
        Some(testInvalidCompanyProfileJourneyDataAsJsObject)
      )

      await(
        TestJourneyDataService.retrieveCompanyProfileAndCtUtr(testJourneyId, testInternalId).failed.map { ex =>
          ex mustBe a[DataAccessException]
          ex.getMessage mustBe s"Error occurred parsing company profile for journey $testJourneyId"
        }
      ) mustBe Succeeded

    }

    "return none for the company profile if the company profile is missing from the journey data" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(
        Some(testMissingCompanyProfileJourneyDataAsJsObject)
      )

      await(TestJourneyDataService.retrieveCompanyProfileAndCtUtr(testJourneyId, testInternalId)) mustBe (None, Some(testCtUtr))

    }

    "raise a DataAccessException if there is an error parsing the Ct Utr" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(Some(testInvalidCtUtrJourneyDataAsJsObject))

      await(
        TestJourneyDataService.retrieveCompanyProfileAndCtUtr(testJourneyId, testInternalId).failed.map { ex =>
          ex mustBe a[DataAccessException]
          ex.getMessage mustBe s"Error occurred parsing Ct UTR for journey $testJourneyId"
        }
      ) mustBe Succeeded

    }

    "return none for the Ct Utr if the Ct Utr is missing from the journey data" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(Some(testMissingCtUtrJourneyDataAsJsObject))

      await(TestJourneyDataService.retrieveCompanyProfileAndCtUtr(testJourneyId, testInternalId)) mustBe (Some(expectedCompanyProfile), None)

    }

    "return None for both company profile and Ct Utr when the journey data is not found" in {

      mockJourneyDataRepository.getJourneyData(testJourneyId, testInternalId) returns Future.successful(None)

      await(TestJourneyDataService.retrieveCompanyProfileAndCtUtr(testJourneyId, testInternalId)) mustBe (None, None)

    }

  }

}
