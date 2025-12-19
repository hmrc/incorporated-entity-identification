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

package uk.gov.hmrc.incorporatedentityidentification.httpparsers

import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse, InternalServerException}
import uk.gov.hmrc.incorporatedentityidentification.models.RegistrationStatus.RegistrationFailuresFormat
import uk.gov.hmrc.incorporatedentityidentification.models.{Failure, Registered, RegistrationFailed, RegistrationStatus}

object RegisterWithMultipleIdentifiersHttpParser {

  val IdentificationKey = "identification"
  val IdentificationTypeKey = "idType"
  val IdentificationValueKey = "idValue"
  val SafeIdKey = "SAFEID"

  implicit object RegisterWithMultipleIdentifiersHttpReads extends HttpReads[RegistrationStatus] {

    override def read(method: String, url: String, response: HttpResponse): RegistrationStatus = {
      response.status match {
        case OK =>
          (for {
            idType <- (response.json \ IdentificationKey \ 0 \ IdentificationTypeKey).validate[String]
            if idType == SafeIdKey
            safeId <- (response.json \ IdentificationKey \ 0 \ IdentificationValueKey).validate[String]
          } yield safeId) match {
            case JsSuccess(safeId, _) => Registered(safeId)
            case _: JsError           => throw new InternalServerException(s"Invalid JSON returned on Register API: ${response.body}")
          }
        case _ =>
          if (response.json.as[JsObject].keys.contains("failures")) {
            (response.json \ "failures").validate[Array[Failure]] match {
              case JsSuccess(failures, _) => RegistrationFailed(Some(failures))
              case _: JsError             => throw new InternalServerException(s"Invalid JSON returned on Register API: ${response.body}")
            }
          } else {
            response.json.validate[Failure] match {
              case JsSuccess(failure, _) => RegistrationFailed(Some(Array(failure)))
              case _: JsError            => throw new InternalServerException(s"Invalid JSON returned on Register API: ${response.body}")
            }
          }
      }
    }
  }

}
