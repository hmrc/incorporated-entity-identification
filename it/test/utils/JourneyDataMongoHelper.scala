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

import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository._

import scala.concurrent.ExecutionContext.Implicits.global

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

  override def beforeEach(): Unit = {
    await(repo.drop)
    super.beforeEach()
  }
}
