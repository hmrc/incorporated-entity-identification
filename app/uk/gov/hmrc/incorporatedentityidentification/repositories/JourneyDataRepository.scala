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

package uk.gov.hmrc.incorporatedentityidentification.repositories

import play.api.libs.json.{Format, JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.JsObjectDocumentWriter
import uk.gov.hmrc.incorporatedentityidentification.config.AppConfig
import uk.gov.hmrc.incorporatedentityidentification.models.IncorporatedEntityIdentificationModel
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository.{authInternalIdKey, _}
import uk.gov.hmrc.mongo.ReactiveRepository

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyDataRepository @Inject()(reactiveMongoComponent: ReactiveMongoComponent,
                                      appConfig: AppConfig)
                                     (implicit ec: ExecutionContext)
  extends ReactiveRepository(
    collectionName = "incorporated-entity-identification",
    mongo = reactiveMongoComponent.mongoConnector.db,
    domainFormat = IncorporatedEntityIdentificationModel.MongoFormat,
    idFormat = implicitly[Format[String]]
  ) {

  def createJourney(journeyId: String, authInternalId: String): Future[String] =
    collection.insert(true).one(
      Json.obj(
        journeyIdKey -> journeyId,
        authInternalIdKey -> authInternalId,
        "creationTimestamp" -> Json.obj("$date" -> Instant.now.toEpochMilli)
      )
    ).map(_ => journeyId)

  def getJourneyData(journeyId: String, authInternalId: String): Future[Option[JsObject]] =
    collection.find(
      Json.obj(
        journeyIdKey -> journeyId,
        authInternalIdKey -> authInternalId
      ),
      Some(Json.obj(
        journeyIdKey -> 0
      ))
    ).one[JsObject]

  def updateJourneyData(journeyId: String, dataKey: String, data: JsValue, authInternalId: String): Future[UpdateWriteResult] =
    collection.update(true).one(
      Json.obj(
        journeyIdKey -> journeyId,
        authInternalIdKey -> authInternalId
      ),
      Json.obj(
        "$set" -> Json.obj(dataKey -> data)
      ),
      upsert = false,
      multi = false
    ).filter(_.n == 1)

  def removeJourneyDataField(journeyId: String, authInternalId: String, dataKey: String): Future[UpdateWriteResult] =
    collection.update(true).one(
      Json.obj(
        journeyIdKey -> journeyId,
        authInternalIdKey -> authInternalId
      ),
      Json.obj(
        "$unset" -> Json.obj(dataKey -> 1)
      ),
      upsert = false,
      multi = false
    ).filter(_.n == 1)

  private lazy val ttlIndex = Index(
    Seq(("creationTimestamp", IndexType.Ascending)),
    name = Some("IncorporatedEntityInformationExpires"),
    options = BSONDocument("expireAfterSeconds" -> appConfig.timeToLiveSeconds)
  )

  private def setIndex(): Unit = {
    collection.indexesManager.drop(ttlIndex.name.get) onComplete {
      _ => collection.indexesManager.ensure(ttlIndex)
    }
  }

  setIndex()

  override def drop(implicit ec: ExecutionContext): Future[Boolean] =
    collection.drop(failIfNotFound = false).map { r =>
      setIndex()
      r
    }
}

object JourneyDataRepository {
  val journeyIdKey: String = "_id"
  val authInternalIdKey: String = "authInternalId"
}
