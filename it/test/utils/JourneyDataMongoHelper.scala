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

package utils

import org.mongodb.scala.model._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.incorporatedentityidentification.models.RegistrationStatus
import uk.gov.hmrc.incorporatedentityidentification.models.error.DataAccessException
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait JourneyDataMongoHelper extends BeforeAndAfterEach {
  self: GuiceOneServerPerSuite with Suite =>

  lazy val repo: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]

  def findById(journeyId: String, authInternalId: String): Option[JsObject] =
    await(repo.getJourneyData(journeyId, authInternalId))

  def insertById(journeyId: String, authInternalId: String, data: JsObject = Json.obj()): Unit =
    await(
      repo.collection
        .insertOne(
          Json.obj(JourneyIdKey -> journeyId, AuthInternalIdKey -> authInternalId) ++ data
        )
        .toFuture()
        .map(_ => ())
    )

  def updateById(journeyId: String, authInternalId: String, dataKey: String, data: JsValue): Unit = {
    await(
      repo.collection.updateOne(
        Filters.and(
          Filters.equal("_id", journeyId),
          Filters.equal("authInternalId", authInternalId)
        ),
        Updates.set(dataKey, Codecs.toBson(data)),
        UpdateOptions().upsert(false)
      ).toFuture().map(_ => ())
    )

  }

  def retrieveRegistrationStatus(journeyId: String, authInternalId: String): Option[RegistrationStatus] =
    findById(journeyId, authInternalId) match {
      case Some(journeyData) => parseRegistrationStatus(journeyId, journeyData)
      case None              => None
    }

  def drop(): Unit = await(repo.drop)

  override def beforeEach(): Unit = {
    await(repo.drop)
    super.beforeEach()
  }

  private def parseRegistrationStatus(journeyId: String, journeyData: JsObject): Option[RegistrationStatus] = {

    if (journeyData.keys.contains("registration")) {
      (journeyData \ "registration").validate[RegistrationStatus] match {
        case JsSuccess(registrationStatus, _) => Some(registrationStatus)
        case _: JsError => throw DataAccessException(s"[VER-5038] Error occurred parsing registration status for journey $journeyId")
      }
    } else None

  }
}
