/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.incorporatedentityidentification.connectors

import javax.inject.Inject
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.incorporatedentityidentification.config.AppConfig
import RegisterWithMultipleIdentifiersHttpParser.{RegisterWithMultipleIdentifiersHttpReads, RegisterWithMultipleIdentifiersResult}

import scala.concurrent.{ExecutionContext, Future}

class RegisterWithMultipleIdentifiersConnector @Inject()(http: HttpClient, appConfig: AppConfig)
                                                        (implicit ec: ExecutionContext) {

  def register(companyNumber: String, ctutr: String)(implicit hc: HeaderCarrier): Future[RegisterWithMultipleIdentifiersResult] = {
    val jsonBody: JsObject =
      Json.obj(
        "company" ->
          Json.obj(
            "crn" -> companyNumber,
            "ctutr" -> ctutr
          )
      )
    http.POST[JsObject, RegisterWithMultipleIdentifiersResult](appConfig.getRegisterWithMultipleIdentifiersUrl, jsonBody)(
      implicitly[Writes[JsObject]],
      RegisterWithMultipleIdentifiersHttpReads,
      hc,
      ec)
  }

}

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
            idType <- (response.json \ IdentificationKey \ IdentificationTypeKey).validate[String]
            if idType == SafeIdKey
            safeId <- (response.json \ IdentificationKey \ IdentificationValueKey).validate[String]
          } yield safeId) match {
            case JsSuccess(safeId, _) => RegisterWithMultipleIdentifiersSuccess(safeId)
            case _: JsError => throw new InternalServerException(s"Invalid JSON returned on Register API: ${response.body}")
          }
        case _ =>
          throw new InternalServerException(s"Invalid response on Register API: ${response.status}, ${response.body}")
      }
    }
  }


  sealed trait RegisterWithMultipleIdentifiersResult

  case class RegisterWithMultipleIdentifiersSuccess(safeId: String) extends RegisterWithMultipleIdentifiersResult

  case class RegisterWithMultipleIdentifiersFailure(status: Int, body: String) extends RegisterWithMultipleIdentifiersResult

}
