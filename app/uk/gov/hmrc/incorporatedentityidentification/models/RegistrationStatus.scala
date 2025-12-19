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

package uk.gov.hmrc.incorporatedentityidentification.models

import play.api.libs.json.*

sealed trait RegistrationStatus

case class Registered(registeredBusinessPartnerId: String) extends RegistrationStatus

case class RegistrationFailed(registrationFailures: Option[Array[Failure]]) extends RegistrationStatus

//case object RegistrationFailed extends RegistrationStatus

case object RegistrationNotCalled extends RegistrationStatus

case class Failure(code: String, reason: String)

object RegistrationStatus {

  val registrationStatusKey = "registrationStatus"

  val registeredBusinessPartnerIdKey = "registeredBusinessPartnerId"
  val RegisteredKey = "REGISTERED"
  val RegistrationFailedKey = "REGISTRATION_FAILED"
  val RegistrationNotCalledKey = "REGISTRATION_NOT_CALLED"
  val registrationFailuresKey = "failures"

  implicit val RegistrationFailuresFormat: OFormat[Failure] = Json.format[Failure]

  implicit val format: OFormat[RegistrationStatus] = new OFormat[RegistrationStatus] {
    override def writes(registrationStatus: RegistrationStatus): JsObject =
      registrationStatus match {
        case Registered(businessPartnerId) =>
          Json.obj(
            registrationStatusKey          -> RegisteredKey,
            registeredBusinessPartnerIdKey -> businessPartnerId
          )
        case RegistrationFailed(failures) =>
          val failuresJson = failures match {
            case Some(errors: Array[Failure]) => Json.obj(registrationFailuresKey -> errors)
            case _                            => Json.obj()
          }
          Json.obj(registrationStatusKey -> RegistrationFailedKey) ++ failuresJson
        case RegistrationNotCalled =>
          Json.obj(registrationStatusKey -> RegistrationNotCalledKey)
      }

    override def reads(json: JsValue): JsResult[RegistrationStatus] =
      (json \ registrationStatusKey).validate[String] match {
        case JsSuccess(RegisteredKey, _) =>
          (json \ registeredBusinessPartnerIdKey).validate[String].map { businessPartnerId =>
            Registered(businessPartnerId)
          }
        case JsSuccess(RegistrationFailedKey, path) =>
          (json \ registrationFailuresKey).validate[Array[Failure]] match {
            case JsSuccess(failures, _) => JsSuccess(RegistrationFailed(Some(failures)), path)
            case _                      => JsSuccess(RegistrationFailed(None), path)
          }
        case JsSuccess(RegistrationNotCalledKey, path) =>
          JsSuccess(RegistrationNotCalled, path)
        case JsSuccess(unknown: String, _) => JsError(JsonValidationError(s"Invalid registration status : $unknown"))
        case _                             => JsError(JsonValidationError(s"Registration status is not of type String"))
      }
  }
}
