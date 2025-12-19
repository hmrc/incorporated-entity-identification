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
import play.api.libs.json.*
import uk.gov.hmrc.incorporatedentityidentification.models.*

class RegistrationStatusSpec extends AnyWordSpec with Matchers {

  val safeId: String = "X00000123456789"
  val invalidPayloadCode: String = "INVALID_PAYLOAD"
  val invalidPayloadReason: String = "Request has not passed validation. Invalid payload."
  val invalidRegimeCode: String = "INVALID_REGIME"
  val invalidRegimePayload: String = "Request has not passed validation. Invalid Regime."

  def serializedRegistrationSuccess(safeId: String): String =
    s"""
       |{
       |    "registrationStatus" : "REGISTERED",
       |    "registeredBusinessPartnerId" : "$safeId"
       |}""".stripMargin

  def serializedSingleFailureRegistration(code: String, reason: String): String =
    s"""
       |{
       |  "registrationStatus":"REGISTRATION_FAILED",
       |  "failures":[{"code":"$code","reason":"$reason"}]
       |}""".stripMargin

  def serialisedMultipleFailureRegistration(firstCode: String, firstReason: String, secondCode: String, secondReason: String): String =
    s"""
       |{
       |  "registrationStatus":"REGISTRATION_FAILED",
       |  "failures" : [
       |    {
       |      "code" : "$firstCode",
       |      "reason" : "$firstReason"
       |    },
       |    {
       |      "code" : "$secondCode",
       |      "reason" : "$secondReason"
       |    }
       |  ]
       |}""".stripMargin

  val serialisedRegistrationNotCalled: String = """{ "registrationStatus" : "REGISTRATION_NOT_CALLED" }"""

  val serialisedRegistrationUnknown: String = """{ "registrationStatus" : "UNKNOWN" }"""

  "RegistrationStatus" should {

    "create an instance of the status Registered from serialized data" in {

      val serialized: JsValue = Json.parse(serializedRegistrationSuccess(safeId))

      serialized.validate[RegistrationStatus] match {
        case JsSuccess(registered, _) => registered mustBe Registered(safeId)
        case error: JsError           => fail(s"Error occurred de-serialising instance of Registered : $error")
      }
    }

    "create an instance of the status RegistrationFailed from serialized data with a single recorded failure" in {

      val serialized: JsValue = Json.parse(serializedSingleFailureRegistration(invalidPayloadCode, invalidPayloadReason))

      serialized.validate[RegistrationStatus] match {
        case JsSuccess(registrationFailed, _) =>
          registrationFailed match {
            case RegistrationFailed(Some(failures)) => failures mustBe Array(Failure(invalidPayloadCode, invalidPayloadReason))
            case _                                  => fail("Error unexpected registration status returned by de-serialisation")
          }
        case error: JsError => fail(s"Error occurred de-serialising registration fail with single failure reason : $error")
      }

    }

    "create an instance of the status RegistrationFailed from serialised data with multiple recorded failures" in {

      val serialized: JsValue =
        Json.parse(serialisedMultipleFailureRegistration(invalidPayloadCode, invalidPayloadReason, invalidRegimeCode, invalidRegimePayload))

      serialized.validate[RegistrationStatus] match {
        case JsSuccess(registrationFailed, _) =>
          registrationFailed match {
            case RegistrationFailed(Some(failures)) =>
              failures mustBe Array(Failure(invalidPayloadCode, invalidPayloadReason), Failure(invalidRegimeCode, invalidRegimePayload))
            case _ => fail("Error unexpected registration status returned by de-serialisation")
          }
        case error: JsError => fail(s"Error occurred de-serialising registration fail with multiple failures : $error")
      }
    }

    "create an instance of status RegistrationNotCalled from serialised data" in {

      val serialised: JsValue = Json.parse(serialisedRegistrationNotCalled)

      serialised.validate[RegistrationStatus] match {
        case JsSuccess(registrationNotCalled, _) => registrationNotCalled mustBe RegistrationNotCalled
        case error: JsError                      => fail(s"Error occurred de-serialising instance of RegistrationNotCalled : $error")
      }
    }

    "raise a JsonValidationError when the registration status is unknown" in {

      val serialised: JsValue = Json.parse(serialisedRegistrationUnknown)

      serialised.validate[RegistrationStatus] match {
        case JsSuccess(registrationStatus, _) =>
          fail(s"Error a valid registration status should not have been returned. Status is $registrationStatus")
        case error: JsError => error.errors.head._2.head.messages.head mustBe "Invalid registration status : UNKNOWN"
      }

    }

    "raise a JsonValidationError when the registration status is of the wrong type" in {

      val serialised: JsValue = JsNumber(10)

      serialised.validate[RegistrationStatus] match {
        case JsSuccess(registrationStatus, _) =>
          fail(s"Error a valid registration status should not have been returned. Status is $registrationStatus")
        case error: JsError => error.errors.head._2.head.messages.head mustBe "Registration status is not of type String"
      }

    }

  }

}
