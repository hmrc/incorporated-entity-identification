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

package uk.gov.hmrc.incorporatedentityidentification.testonly


import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RegisterWithMultipleIdentifiersStubController @Inject()(controllerComponents: ControllerComponents) extends BackendController(controllerComponents) {

  val singleFailureResultAsString: String =
    s"""{
       |  "code" : "INVALID_PAYLOAD",
       |  "reason" : "Request has not passed validation. Invalid payload."
       |}""".stripMargin

  val multipleFailureResultAsString: String =
    s"""
       |{
       |    "failures" : [
       |      {
       |        "code" : "INVALID_PAYLOAD",
       |        "reason" : "Request has not passed validation. Invalid payload."
       |      },
       |      {
       |        "code" : "INVALID_REGIME",
       |        "reason" : "Request has not passed validation. Invalid Regime."
       |      }
       |    ]
       |}""".stripMargin

  val singleFailureResponseAsJson: JsObject = Json.parse(singleFailureResultAsString).as[JsObject]
  val multipleFailureResponseAsJson: JsObject = Json.parse(multipleFailureResultAsString).as[JsObject]

  def registerWithMultipleIdentifiers(): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val ctutr: String = (request.body \\ "ctutr").map(_.as[String]).head

      val stubbedSafeId = {
        if (e2eTestData.contains(ctutr)) e2eTestData(ctutr)
        else "X00000123456789"
      }

      ctutr match {
        case "1111111111" => Future.successful(BadRequest(singleFailureResultAsString))
        case "2222222222" => Future.successful(BadRequest(multipleFailureResponseAsJson))
        case _ => Future.successful(Ok(createSuccessResponse(stubbedSafeId)))
      }
  }

  private def createSuccessResponse(stubbedSafeId: String): JsObject =
    Json.obj(
      "identification" -> Json.arr(
        Json.obj(
          "idType" -> "SAFEID",
          "idValue" -> stubbedSafeId
        )
      )
    )

  //PPT Test Data
  val e2eTestData = Map(
    "2111234408" -> "XW0000100382303",
    "9111234409" -> "XA0000100382304",
    "1111234384" -> "XP0000100382278",
    "8111234385" -> "XQ0000100382279",
    "6111234386" -> "XJ0000100382280",
    "4111234387" -> "XK0000100382281",
    "2111234388" -> "XL0000100382282",
    "2111234389" -> "XM0000100382283",
    "4111234390" -> "XN0000100382284",
    "2111234391" -> "XY0000100382285",
    "2111234392" -> "XP0000100382286",
    "9111234393" -> "XQ0000100382287",
    "7111234394" -> "XR0000100382288",
    "5111234395" -> "XS0000100382289",
    "3111234396" -> "XL0000100382290",
    "1111234397" -> "XM0000100382291",
    "1111234398" -> "XN0000100382292",
    "8111234399" -> "XY0000100382293",
    "5111234400" -> "XP0000100382294",
    "3111234401" -> "XQ0000100382295",
    "1111234402" -> "XS0000100382297",
    "1111234403" -> "XT0000100382298",
    "2767776722" -> "XG0000100384737", // Further PPT test data
    "9767776723" -> "XH0000100384738",
    "7767776724" -> "XX0000100384739",
    "5767776725" -> "XB0000100384740",
    "3767776726" -> "XC0000100384741",
    "1767776727" -> "XD0000100384742",
    "1767776728" -> "XE0000100384743",
    "8767776729" -> "XG0000100384745",
    "1767776730" -> "XH0000100384746",
    "1767776731" -> "XX0000100384747"
  )
}
