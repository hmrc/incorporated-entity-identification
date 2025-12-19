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

package models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.incorporatedentityidentification.models.BusinessVerificationStatus.*
import uk.gov.hmrc.incorporatedentityidentification.models.*

class BusinessVerificationStatusSpec extends AnyWordSpec with Matchers {

  def serializedBusinessVerificationStatus(status: String): String = s"""{ "verificationStatus" : "$status" }"""

  "BusinessVerificationStatus" should {

    "create an instance of the status BusinessVerificationPass from serialized data" in {

      val serialized: JsValue = Json.parse(serializedBusinessVerificationStatus(businessVerificationPassKey))

      serialized.validate[BusinessVerificationStatus] match {
        case JsSuccess(status, _) => status mustBe BusinessVerificationPass
        case error: JsError       => fail(s"An error occurred parsing the serialized data : $error")
      }
    }

    "create an instance of the status BusinessVerificationFail from serialized data" in {

      val serialized: JsValue = Json.parse(serializedBusinessVerificationStatus(businessVerificationFailKey))

      serialized.validate[BusinessVerificationStatus] match {
        case JsSuccess(status, _) => status mustBe BusinessVerificationFail
        case error: JsError       => fail(s"An error occurred parsing the serialized data : $error")
      }

    }

    "create an instance of the status BusinessVerificationNotEnoughInformationToChallenge from serialized data" in {

      val serialized: JsValue = Json.parse(serializedBusinessVerificationStatus(businessVerificationNotEnoughInfoToChallengeKey))

      serialized.validate[BusinessVerificationStatus] match {
        case JsSuccess(status, _) => status mustBe BusinessVerificationNotEnoughInformationToChallenge
        case error: JsError       => fail(s"An error occurred parsing the serialized data : $error")
      }

    }

    "create an instance of the status BusinessVerificationNotEnoughInformationToCallBV from serialized data" in {

      val serialized: JsValue = Json.parse(serializedBusinessVerificationStatus(businessVerificationNotEnoughInfoToCallBVKey))

      serialized.validate[BusinessVerificationStatus] match {
        case JsSuccess(status, _) => status mustBe BusinessVerificationNotEnoughInformationToCallBV
        case error: JsError       => fail(s"An error occurred parsing the serialized data : $error")
      }

    }

    "create an instance of the status CtEnrolled from serialized data" in {

      val serialized: JsValue = Json.parse(serializedBusinessVerificationStatus(businessVerificationCtEnrolledKey))

      serialized.validate[BusinessVerificationStatus] match {
        case JsSuccess(status, _) => status mustBe CtEnrolled
        case error: JsError       => fail(s"An error occurred parsing the serialized data : $error")
      }

    }
  }

}
