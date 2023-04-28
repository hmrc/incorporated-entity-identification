/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json, OFormat}
import uk.gov.hmrc.http.{HttpReads, HttpResponse, InternalServerException}

object RegisterWithMultipleIdentifiersHttpParser {

  val IdentificationKey = "identification"
  val IdentificationTypeKey = "idType"
  val IdentificationValueKey = "idValue"
  val SafeIdKey = "SAFEID"

  implicit object RegisterWithMultipleIdentifiersHttpReads extends HttpReads[RegisterWithMultipleIdentifiersResult] {

    override def read(method: String, url: String, response: HttpResponse): RegisterWithMultipleIdentifiersResult = {
      response.status match {
        case OK =>
          (for {
            idType <- (response.json \ IdentificationKey \ 0 \ IdentificationTypeKey).validate[String]
            if idType == SafeIdKey
            safeId <- (response.json \ IdentificationKey \ 0 \ IdentificationValueKey).validate[String]
          } yield safeId) match {
            case JsSuccess(safeId, _) => RegisterWithMultipleIdentifiersSuccess(safeId)
            case _: JsError => throw new InternalServerException(s"Invalid JSON returned on Register API: ${response.body}")
          }
        case _ => if (response.json.as[JsObject].keys.contains("failures")) {
          (response.json \ "failures").validate[Array[Failures]] match {
            case JsSuccess(failures, _) => RegisterWithMultipleIdentifiersFailure(response.status, failures)
            case _: JsError => throw new InternalServerException(s"Invalid JSON returned on Register API: ${response.body}") }
        } else {
          response.json.validate[Failures] match {
            case JsSuccess(failure, _) => RegisterWithMultipleIdentifiersFailure(response.status, Array(failure))
            case _: JsError => throw new InternalServerException(s"Invalid JSON returned on Register API: ${response.body}") } }
      }
    }
  }

  sealed trait RegisterWithMultipleIdentifiersResult

  case class RegisterWithMultipleIdentifiersSuccess(safeId: String) extends RegisterWithMultipleIdentifiersResult

  case class RegisterWithMultipleIdentifiersFailure(status: Int, body: Array[Failures]) extends RegisterWithMultipleIdentifiersResult

  case class Failures(code: String, reason: String)

  implicit val format: OFormat[Failures] = Json.format[Failures]
}
