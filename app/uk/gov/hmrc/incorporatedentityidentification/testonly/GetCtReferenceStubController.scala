/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}

@Singleton
class GetCtReferenceStubController @Inject()(controllerComponents: ControllerComponents) extends BackendController(controllerComponents) {

  def getCtReference(companyNumber: String): Action[AnyContent] = Action {
    companyNumber match {
      case "00000000" =>
        NotFound(
          Json.obj(
            "code" -> "NOT_FOUND",
            "reason" -> "The back end has indicated that CT UTR cannot be returned"
          )
        )
      case crn if e2eTestData.contains(crn) =>
        Ok(Json.obj("CTUTR" -> e2eTestData(crn)))
      case "99999999" =>
        Ok(Json.obj("CTUTR" -> "0987654321"))
      case "21436587" => // VRS Partnerships E2E testing
        Ok(Json.obj("CTUTR" -> "5432167812"))
      case "11111111" =>
        Ok(Json.obj("CTUTR" -> "1111111111"))
      case "22222222" =>
        Ok(Json.obj("CTUTR" -> "2222222222"))
      case _ =>
        Ok(Json.obj("CTUTR" -> "1234567890"))
    }
  }

  //To be removed after E2E testing
  val e2eTestData = Map(
    "99999991" -> "1044814810",
    "99999992" -> "8851208889",
    "99999993" -> "2251904531",
    "99999994" -> "8754000033",
    "99999995" -> "8901324101",
    "99999996" -> "7033600713",
    "99999997" -> "1000000230",
    "99999998" -> "1000000231",
    "99999990" -> "1000000005",
    "99999911" -> "3332221111",
    "99999912" -> "1112223333",
    "99999913" -> "1112223334",
    "99999914" -> "3332221110",
    "91000051" -> "8202107245",
    "10355204" -> "3202109220",
    "10355205" -> "1202109221",
    "10355206" -> "1202109222",
    "10355207" -> "8202109223",
    "10355208" -> "6202109224",
    "10357204" -> "4202110081",
    "10357205" -> "2202110082",
    "10357206" -> "2202110083",
    "10357207" -> "9202110084",
    "10357208" -> "7202110085",
    "10357210" -> "5202110086",
    "10357211" -> "3202110087",
    "10357212" -> "1202110088",
    "10357213" -> "1202110089",
    "10357214" -> "3202110090",
    "10382303" -> "2111234408",
    "10382304" -> "9111234409", // PPT Test Data
    "10382278" -> "1111234384",
    "10382279" -> "8111234385",
    "10382280" -> "6111234386",
    "10382281" -> "4111234387",
    "10382282" -> "2111234388",
    "10382283" -> "2111234389",
    "10382284" -> "4111234390",
    "10382285" -> "2111234391",
    "10382286" -> "2111234392",
    "10382287" -> "9111234393",
    "10382288" -> "7111234394",
    "10382289" -> "5111234395",
    "10382290" -> "3111234396",
    "10382291" -> "1111234397",
    "10382292" -> "1111234398",
    "10382293" -> "8111234399",
    "10382294" -> "5111234400",
    "10382295" -> "3111234401",
    "10382297" -> "1111234402",
    "10382298" -> "1111234403",
    "10384737" -> "2767776722", //Further PPT Test Data
    "10384738" -> "9767776723",
    "10384739" -> "7767776724",
    "10384740" -> "5767776725",
    "10384741" -> "3767776726",
    "10384742" -> "1767776727",
    "10384743" -> "1767776728",
    "10384745" -> "8767776729",
    "10384746" -> "1767776730",
    "10384747" -> "1767776731"
  )
}
