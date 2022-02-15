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


import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RegisterWithMultipleIdentifiersStubController @Inject()(controllerComponents: ControllerComponents) extends BackendController(controllerComponents) {

  def registerWithMultipleIdentifiers(): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val ctutr: String = (request.body \\ "ctutr").map(_.as[String]).head

      val stubbedSafeId = {
        if (e2eTestData.contains(ctutr)) e2eTestData(ctutr)
        else "X00000123456789"
      }

      Future.successful(Ok(Json.obj(
        "identification" -> Json.arr(
          Json.obj(
            "idType" -> "SAFEID",
            "idValue" -> stubbedSafeId
          )
        ))))
  }

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
    "1111234403" -> "XT0000100382298"
  )
}
