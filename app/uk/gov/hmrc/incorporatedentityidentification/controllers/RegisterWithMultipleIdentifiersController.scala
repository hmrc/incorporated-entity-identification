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

package uk.gov.hmrc.incorporatedentityidentification.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.incorporatedentityidentification.connectors.RegisterWithMultipleIdentifiersHttpParser.{RegisterWithMultipleIdentifiersFailure, RegisterWithMultipleIdentifiersSuccess}
import uk.gov.hmrc.incorporatedentityidentification.models.RegisterWithMultipleIdentifiersModel
import uk.gov.hmrc.incorporatedentityidentification.services.RegisterWithMultipleIdentifiersService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class RegisterWithMultipleIdentifiersController @Inject()(cc: ControllerComponents,
                                                          registerWithMultipleIdentifiersService: RegisterWithMultipleIdentifiersService,
                                                          val authConnector: AuthConnector,
                                                         )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def register(): Action[RegisterWithMultipleIdentifiersModel] = Action.async(parse.json[RegisterWithMultipleIdentifiersModel]) {
    implicit request =>
      authorised() {
        registerWithMultipleIdentifiersService.register(request.body.company.crn, request.body.company.ctutr).map {
          case RegisterWithMultipleIdentifiersSuccess(safeId) =>
            Ok(Json.obj(
              "registration" -> Json.obj(
                "registrationStatus" -> "REGISTERED",
                "registeredBusinessPartnerId" -> safeId)))
          case RegisterWithMultipleIdentifiersFailure(status, body) =>
            Ok(Json.obj(
              "registration" -> Json.obj(
                "registrationStatus" -> "REGISTRATION_FAILED")))
        }

      }

  }

}
