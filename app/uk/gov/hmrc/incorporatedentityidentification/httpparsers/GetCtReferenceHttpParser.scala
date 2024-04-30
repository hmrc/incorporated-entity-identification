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

import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.http.{BadGatewayException, HttpException, HttpReads, HttpResponse, NotFoundException}

import scala.util.Try

object GetCtReferenceHttpParser {

  val ctutrKey = "CTUTR"

  implicit object GetCtReferenceHttpReads extends HttpReads[Either[HttpException, String]] {
    override def read(method: String, url: String, response: HttpResponse): Either[HttpException, String] = {
      response.status match {
        case OK =>
          val result = for {
            json  <- Try(response.json).toEither.left.map(_.getMessage take 40)
            ctutr <- (json \ ctutrKey).validate[String].asEither.left.map(_.toString)
          } yield ctutr

          result.left.map { errors =>
            new BadGatewayException(s"HoD returned a malformed JSON on $method <$url> errors: $errors")
          }
        case NOT_FOUND =>
          Left(new NotFoundException(s"HoD has indicated that CT UTR cannot be returned on $method <$url>"))
        case status =>
          Left(new BadGatewayException(s"HoD returned status code <$status> on $method <$url>"))
      }
    }
  }
}
