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

package uk.gov.hmrc.incorporatedentityidentification.testonly

import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RegisterWithMultipleIdentifiersStubController @Inject() (controllerComponents: ControllerComponents)
    extends BackendController(controllerComponents) {

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

  def registerWithMultipleIdentifiers(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val ctutr: String = (request.body \\ "ctutr").map(_.as[String]).head

    val stubbedSafeId = {
      if (e2eTestData.contains(ctutr)) e2eTestData(ctutr)
      else "X00000123456789"
    }

    ctutr match {
      case "1111111111" => Future.successful(BadRequest(singleFailureResultAsString))
      case "2222222222" => Future.successful(BadRequest(multipleFailureResponseAsJson))
      case _            => Future.successful(Ok(createSuccessResponse(stubbedSafeId)))
    }
  }

  private def createSuccessResponse(stubbedSafeId: String): JsObject =
    Json.obj(
      "identification" -> Json.arr(
        Json.obj(
          "idType"  -> "SAFEID",
          "idValue" -> stubbedSafeId
        )
      )
    )

  // PPT Test Data
  lazy private val plasticPackagingTaxData: Map[String, String] = Map(
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
    "1767776731" -> "XX0000100384747",
    "5177015315" -> "XW0000100071290",
    "9220316469" -> "XA0000100082579",
    "9177001494" -> "XP0000100415578"
  )

  lazy private val atsData: Map[String, String] = Map(
    "1004481479" -> "XP0000100448147",
    "8899001122" -> "X0000100448129",
    "8800442962" -> "XY0000100442962",
    "6600442946" -> "XY0000100442946",
    "8800431892" -> "XX0000100431892",
    "8800431891" -> "XH0000100431891",
    "9100431887" -> "XL0000100431887",
    "8800431880" -> "XE0000100431880",
    "2203405620" -> "XW0000100294820",
    "1962476745" -> "XL0000100294670",
    "1140236788" -> "XM0000100294600",
    "7144440039" -> "XZ0000100294535",
    "2119102033" -> "XA0000100294368",
    "1144440208" -> "XL0000100028993",
    "2187647873" -> "XW0000100029017",
    "1113456543" -> "XS0000100029021",
    "4111112233" -> "XT0000800004950",
    "9737040035" -> "XZ0000800004781",
    "5177018487" -> "X000800000044",
    "9882912946" -> "XZ0000100462586",
    "1087915528" -> "XD0000100462513",
    "1389118099" -> "XQ0000100462477",
    "6139626122" -> "XY0000100462475",
    "4098533566" -> "XH0000100462177",
    "3481282169" -> "XY0000100462024",
    "1003744109" -> "XE0000100374410",
    "9900883464" -> "XB0000100293132",
    "9562356894" -> "XZ0000100293023",
    "4569875412" -> "XR0000100293020",
    "1002836857" -> "XB0000100283685",
    "9933110010" -> "XL0000100283608",
    "9900883458" -> "XT0000100283132",
    "9900883457" -> "XS0000100283131",
    "9900883456" -> "XR0000100283130",
    "9911772342" -> "XA0000100283128",
    "9911772340" -> "XW0000100283127",
    "8723561231" -> "XV0000100283126",
    "3320658741" -> "XG0000100282089",
    "1212141510" -> "XD0000100281011",
    "9100280902" -> "XC0000100280902",
    "4043617554" -> "XR0000100278759",
    "7827727070" -> "XQ0000100278758"
  )

  lazy private val e2eTestData: Map[String, String] = plasticPackagingTaxData ++ atsData
}
