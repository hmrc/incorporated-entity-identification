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

package uk.gov.hmrc.incorporatedentityidentification.repositories

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model._

import play.api.libs.json._

import uk.gov.hmrc.incorporatedentityidentification.config.AppConfig
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository._

import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyDataRepository @Inject()(mongoComponent: MongoComponent, appConfig: AppConfig)
                                     (implicit ec: ExecutionContext) extends PlayMongoRepository[JsObject](
  collectionName = "incorporated-entity-identification",
  mongoComponent = mongoComponent,
  domainFormat = implicitly[Format[JsObject]],
  indexes = Seq(timeToLiveIndex(appConfig.timeToLiveSeconds))
){

  def createJourney(journeyId: String, authInternalId: String): Future[String] =
    collection.insertOne(
      Json.obj(
        JourneyIdKey -> journeyId,
        AuthInternalIdKey -> authInternalId,
        CreationTimestampKey -> Json.obj("$date" -> Instant.now.toEpochMilli)
      )
    ).toFuture().map(_ => journeyId)

  def getJourneyData(journeyId: String, authInternalId: String): Future[Option[JsObject]] =
    collection.find(
      filterJourneyConfig(journeyId, authInternalId)
    ).headOption()

  def updateJourneyData(journeyId: String, dataKey: String, data: JsValue, authInternalId: String): Future[Boolean] =
    collection.updateOne(
      filterJourneyConfig(journeyId, authInternalId),
      Updates.set(dataKey, Codecs.toBson(data)),
      UpdateOptions().upsert(false)
    ).toFuture.map {
      _.getMatchedCount == 1
    }

  def removeJourneyDataField(journeyId: String, authInternalId: String, dataKey: String): Future[Boolean] =
    collection.updateOne(
      filterJourneyConfig(journeyId, authInternalId),
      Updates.unset(dataKey)
    ).toFuture.map {
      _.getMatchedCount == 1
    }

  def removeJourneyData(journeyId: String, authInternalId: String): Future[Boolean] =
    collection.findOneAndReplace(
      filterJourneyConfig(journeyId, authInternalId),
      Json.obj(
        JourneyIdKey -> journeyId,
        AuthInternalIdKey -> authInternalId,
        CreationTimestampKey -> Json.obj("$date" -> Instant.now.toEpochMilli)
      )
    ).toFuture.map {
      _ != null
    }

  def drop: Future[Unit] = collection.drop().toFuture.map(_ => Unit)

  private def filterJourneyConfig(journeyId: String, authInternalId: String): Bson =
    Filters.and(
      Filters.equal(JourneyIdKey, journeyId),
      Filters.equal(AuthInternalIdKey, authInternalId)
    )
}

object JourneyDataRepository {
  val JourneyIdKey: String = "_id"
  val AuthInternalIdKey: String = "authInternalId"
  val CreationTimestampKey: String = "creationTimestamp"

  def timeToLiveIndex(timeToLiveDuration: Long): IndexModel = IndexModel(
    keys = ascending(CreationTimestampKey),
    indexOptions = IndexOptions()
      .name("IncorporatedEntityInformationExpires")
      .expireAfter(timeToLiveDuration, TimeUnit.SECONDS)
  )
}
