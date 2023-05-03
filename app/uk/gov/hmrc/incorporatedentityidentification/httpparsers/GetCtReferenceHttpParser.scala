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

import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{BadGatewayException, HttpReads, HttpResponse}

import scala.util.{Failure, Success, Try}

object GetCtReferenceHttpParser {

  val ctutrKey = "CTUTR"

  implicit object GetCtReferenceHttpReads extends HttpReads[Option[String]] {
    override def read(method: String, url: String, response: HttpResponse): Option[String] = {
      response.status match {
        case OK =>
          Try(response.json) match {
            case Failure(exception) =>
              throw new BadGatewayException(s"HoD returned a malformed JSON on $method <$url> errors: ${exception.getMessage}")
            case Success(json) =>
              (json \ ctutrKey).validate[String] match {
                case JsSuccess(ctutr, _) =>
                  Some(ctutr)
                case JsError(errors) =>
                  throw new BadGatewayException(s"HoD returned a malformed JSON on $method <$url> errors: $errors")
              }
          }
        case NOT_FOUND =>
          None
        case status =>
          throw new BadGatewayException(s"HoD returned status code <$status> on $method <$url>")
      }
    }
  }
}
