/*
 * Copyright 2021 HM Revenue & Customs
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
      case _ =>
        Ok(Json.obj("CTUTR" -> "1234567890"))
    }
  }

  //To be removed after E2E testing
  val e2eTestData = Map(
    "91000051" -> "8202107245"
  )
}
