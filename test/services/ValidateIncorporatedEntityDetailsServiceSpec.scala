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

package services

import org.mockito.scalatest.{IdiomaticMockito, ResetMocksAfterEachTest}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incorporatedentityidentification.connectors.GetCtReferenceConnector
import uk.gov.hmrc.incorporatedentityidentification.models.{DetailsMatched, DetailsMismatched, DetailsNotFound}
import uk.gov.hmrc.incorporatedentityidentification.services.ValidateIncorporatedEntityDetailsService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ValidateIncorporatedEntityDetailsServiceSpec extends AnyWordSpec with Matchers with IdiomaticMockito with ResetMocksAfterEachTest {
  val mockGetCtReferenceConnector: GetCtReferenceConnector = mock[GetCtReferenceConnector]

  object TestValidateIncorporateEntityDetailsService extends ValidateIncorporatedEntityDetailsService(
    mockGetCtReferenceConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val testCompanyNumber = "testCompanyNumber"
  val testCtReference = "testCtReference"

  "validateDetails" should {
    s"return $DetailsMatched" when {
      "the supplied CT Reference matches the stored CT Reference" in {
        mockGetCtReferenceConnector.getCtReference(eqTo(testCompanyNumber)) returns Future.successful(Some(testCtReference))

        await(TestValidateIncorporateEntityDetailsService.validateDetails(testCompanyNumber, Some(testCtReference))) mustBe DetailsMatched
      }
    }
    s"return $DetailsMismatched" when {

      "the supplied CT Reference does not match the stored CT Reference" in {
        val mismatchedTestCtReference = "mismatchedTestCtReference"

        mockGetCtReferenceConnector.getCtReference(eqTo(testCompanyNumber)) returns Future.successful(Some(testCtReference))

        await(TestValidateIncorporateEntityDetailsService.validateDetails(testCompanyNumber, Some(mismatchedTestCtReference))) mustBe DetailsMismatched
      }

      "the user asserts the unincorporated association does not have a Ct Utr, but one is found" in {

        mockGetCtReferenceConnector.getCtReference(eqTo(testCompanyNumber)) returns Future.successful(Some(testCtReference))

        await(TestValidateIncorporateEntityDetailsService.validateDetails(testCompanyNumber, None)) mustBe DetailsMismatched
      }

    }
    s"return $DetailsNotFound" when {
      "there is no stored CT Reference for the provided Company Number" in {
        mockGetCtReferenceConnector.getCtReference(eqTo(testCompanyNumber)) returns Future.successful(None)

        await(TestValidateIncorporateEntityDetailsService.validateDetails(testCompanyNumber, Some(testCtReference))) mustBe DetailsNotFound
      }
    }

  }


}
