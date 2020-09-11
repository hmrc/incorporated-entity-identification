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
import uk.gov.hmrc.incorporatedentityidentification.services.JourneyDataService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class JourneyDataController @Inject()(cc: ControllerComponents,
                                      journeyDataService: JourneyDataService)
                                     (implicit ec: ExecutionContext) extends BackendController(cc) {

  def createJourney(): Action[AnyContent] = Action.async {
    val journeyIdKey = "journeyId"
    journeyDataService.createJourney().map {
      journeyId => Created(Json.obj(journeyIdKey -> journeyId))
    }
  }

  def getJourneyData(journeyId: String): Action[AnyContent] = Action.async {
    journeyDataService.getJourneyData(journeyId).map {
      case Some(journeyData) => Ok(journeyData)
      case None => NotFound(Json.obj(
        "code" -> "NOT_FOUND",
        "reason" -> s"No data exists for journey ID: $journeyId"
      ))
    }
  }

  def getJourneyDataByKey(journeyId: String, dataKey: String): Action[AnyContent] = Action.async {
    journeyDataService.getJourneyData(journeyId, dataKey).map {
      case Some(journeyData) => Ok(journeyData)
      case None => NotFound(Json.obj(
        "code" -> "NOT_FOUND",
        "reason" -> s"No data exists for either journey ID: $journeyId or data key: $dataKey"
      ))
    }
  }

  def updateJourneyData(journeyId: String, dataKey: String): Action[JsValue] = Action.async(parse.json) {
    req =>
      journeyDataService.updateJourneyData(journeyId, dataKey, req.body)
        .map(_ => Ok)
  }
}
