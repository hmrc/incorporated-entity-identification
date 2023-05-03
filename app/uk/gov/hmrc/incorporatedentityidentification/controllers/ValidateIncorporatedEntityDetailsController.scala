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

package uk.gov.hmrc.incorporatedentityidentification.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.BadGatewayException
import uk.gov.hmrc.incorporatedentityidentification.models.{DetailsMatched, DetailsMismatched, DetailsNotFound, IncorporatedEntityDetailsModel}
import uk.gov.hmrc.incorporatedentityidentification.services.ValidateIncorporatedEntityDetailsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidateIncorporatedEntityDetailsController @Inject()(cc: ControllerComponents,
                                                            validateIncorporatedEntityDetailsService: ValidateIncorporatedEntityDetailsService,
                                                            val authConnector: AuthConnector
                                                           )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def validateDetails(): Action[IncorporatedEntityDetailsModel] = Action.async(parse.json[IncorporatedEntityDetailsModel]) {
    implicit request =>
      authorised() {
        validateIncorporatedEntityDetailsService.validateDetails(request.body.companyNumber, request.body.ctutr).map {
          case DetailsMatched =>
            Ok(Json.obj("matched" -> true))
          case DetailsMismatched =>
            Ok(Json.obj("matched" -> false))
          case DetailsNotFound =>
            NotFound(Json.obj("code" -> "NOT_FOUND", "reason" -> "The back end has indicated that CT UTR cannot be returned"))
        }.recoverWith {
          case e: BadGatewayException =>
            Future.apply(BadGateway(Json.obj("code" -> "BAD_GATEWAY", "reason" -> e.getMessage)))
        }
      }
  }

}
