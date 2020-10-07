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

package repositories

import org.scalatest.concurrent.{AbstractPatienceConfiguration, Eventually}
import org.scalatest.time.{Seconds, Span}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.incorporatedentityidentification.models.IncorporatedEntityIdentificationModel
import utils.ComponentSpecHelper
import assets.TestConstants.testJourneyId
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository
import utils.WiremockHelper.scaled

import scala.concurrent.ExecutionContext.Implicits.global

class JourneyDataRepositoryISpec extends ComponentSpecHelper with AbstractPatienceConfiguration with Eventually{

    override lazy val app: Application = new GuiceApplicationBuilder()
      .in(Environment.simple(mode = Mode.Dev))
      .configure(config)
      .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
      .configure("mongodb.timeToLiveSeconds" -> "10")
      .build

    val repo: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]

    "documents" should {
      "expire" in {
        val journeyId = testJourneyId
        val journeyConfig = "continueURL"
        await(repo.createJourney(journeyId, None))
        implicit val patienceConfig: PatienceConfig =
          PatienceConfig(timeout = scaled(Span(90, Seconds)), interval = scaled(Span(10, Seconds)))
        eventually {
          await(repo.findById(journeyId)) mustBe None
        }
      }
    }

  }

