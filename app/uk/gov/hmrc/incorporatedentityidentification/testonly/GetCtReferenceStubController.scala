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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}

@Singleton
class GetCtReferenceStubController @Inject() (controllerComponents: ControllerComponents) extends BackendController(controllerComponents) {

  def getCtReference(companyNumber: String): Action[AnyContent] = Action {
    companyNumber match {
      case "00000000" =>
        NotFound(
          Json.obj(
            "code"   -> "NOT_FOUND",
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

  lazy val e2eTestData: Map[String, String] = plasticTaxTestData ++ atsTestData

  private val plasticTaxTestData: Map[String, String] = Map(
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
    "68834011" -> "1364655464",
    "83409443" -> "3177020531",
    "76543216" -> "2177016197",
    "76543217" -> "4177019843",
    "23456910" -> "1177015401",
    "23456930" -> "2177014466",
    "23456798" -> "4177005640",
    "23456799" -> "2177009464",
    "234568OO" -> "2177007924",
    "23456801" -> "2548822219",
    "23456925" -> "7177020803",
    "77221234" -> "6177000601",
    "23456816" -> "3177022103",
    "23456935" -> "5177015315",
    "23456936" -> "8177015384",
    "23456937" -> "9177001494",
    "CRN08444" -> "3300555807",
    "9870551"  -> "8177017961",
    "9870552"  -> "9177021920",
    "9870553"  -> "6177009072",
    "76543219" -> "1220316477",
    "76543220" -> "1220316476",
    "76543225" -> "9220316469",
    "36123619" -> "6220316448",
    "9870562"  -> "7107807868",
    "9870563"  -> "1220316446",
    "9870564"  -> "7127001773",
    "9870565"  -> "7161525510"
  )

  private val atsTestData: Map[String, String] = Map(
    "66778899" -> "1004481479",
    "88990011" -> "8899001122",
    "77442962" -> "8800442962",
    "77442946" -> "6600442946",
    "77431892" -> "8800431892",
    "77431891" -> "8800431891",
    "80431887" -> "9100431887",
    "77431880" -> "8800431880",
    "34561"    -> "2203405620",
    "12345610" -> "1962476745",
    "23165475" -> "1140236788",
    "45582365" -> "7144440039",
    "78787885" -> "2119102033",
    "31257555" -> "1144440208",
    "31757555" -> "2187647873",
    "12575555" -> "1113456543",
    "23456789" -> "4111112233",
    "8000005"  -> "9737040035",
    "34567890" -> "5177018487",
    "79939352" -> "9882912946",
    "94849225" -> "1087915528",
    "29584404" -> "1389118099",
    "45781522" -> "6139626122",
    "25263733" -> "4098533566",
    "54936458" -> "3481282169",
    "41552412" -> "1003744109",
    "99008844" -> "9900883464",
    "6568974"  -> "9562356894",
    "56987421" -> "4569875412",
    "10028368" -> "1002836857",
    "99912300" -> "9933110010",
    "99008837" -> "9900883458",
    "99008836" -> "9900883457",
    "99008835" -> "9900883456",
    "99117724" -> "9911772342",
    "99117723" -> "9911772340",
    "88226611" -> "8723561231",
    "12345610" -> "1962476745",
    "34562451" -> "3320658741",
    "33205148" -> "1212141510",
    "99280902" -> "9100280902",
    "91621428" -> "4043617554",
    "56026007" -> "7827727070"
  )

}
