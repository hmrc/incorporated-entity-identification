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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.internalId
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.incorporatedentityidentification.services.JourneyDataService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyDataController @Inject()(cc: ControllerComponents,
                                      journeyDataService: JourneyDataService,
                                      val authConnector: AuthConnector)
                                     (implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def createJourney(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(internalId) { internalId =>
        val journeyIdKey = "journeyId"
        journeyDataService.createJourney(internalId).map {
          journeyId => Created(Json.obj(journeyIdKey -> journeyId))
        }
      }
  }

  def getJourneyData(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(internalId) { internalId =>
        journeyDataService.getJourneyData(journeyId).flatMap {
          case Some(journeyData) =>
            journeyDataService.getStoredAuthInternalId(journeyId).map {
              authInternalId =>
                if (authInternalId == internalId) Ok(journeyData)
                else Forbidden(Json.obj(
                  "code" -> "FORBIDDEN",
                  "reason" -> "Auth Internal IDs do not match"))
            }
          case None => Future.successful(NotFound(Json.obj(
            "code" -> "NOT_FOUND",
            "reason" -> s"No data exists for journey ID: $journeyId"
          )))
        }
      }
  }

  def getJourneyDataByKey(journeyId: String, dataKey: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(internalId) { internalId =>
        journeyDataService.getStoredAuthInternalId(journeyId).flatMap {
          authInternalId =>
            if (authInternalId == internalId) {
              journeyDataService.getJourneyData(journeyId, dataKey).map {
                case Some(journeyData) => Ok(journeyData)
                case None => NotFound(Json.obj("code" -> "NOT_FOUND",
                  "reason" -> s"No data exists for either journey ID: $journeyId or data key: $dataKey"))
              }
            }
            else Future.successful(Forbidden(Json.obj(
              "code" -> "FORBIDDEN",
              "reason" -> "Auth Internal IDs do not match")))
        }
      }
  }

  def updateJourneyData(journeyId: String, dataKey: String): Action[JsValue] = Action.async(parse.json) {
    implicit req =>
      authorised().retrieve(internalId) { internalId =>
        journeyDataService.updateJourneyData(journeyId, dataKey, req.body).flatMap(
          _ => {
            journeyDataService.getStoredAuthInternalId(journeyId).map {
              authInternalId =>
                if (authInternalId == internalId) Ok
                else Forbidden(Json.obj("code" -> FORBIDDEN,
                  "reason" -> "Auth Internal IDs do not match"))
            }
          }
        )
      }
  }
}
