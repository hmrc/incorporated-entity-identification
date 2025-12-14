/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incorporatedentityidentification.config.AppConfig
import uk.gov.hmrc.incorporatedentityidentification.connectors.RegisterWithMultipleIdentifiersConnector
import uk.gov.hmrc.incorporatedentityidentification.models.*
import uk.gov.hmrc.incorporatedentityidentification.services.{JourneyDataService, RegisterWithMultipleIdentifiersService}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class TestAppConfig(config: Configuration, servicesConfig: ServicesConfig) extends AppConfig(config, servicesConfig) {

  override lazy val registrationTimeout: Long = 2000

  val retryInterval: Long = 200

  override def determineRetryInterval(): Long = {

    if (retryInterval < registrationTimeout) retryInterval else registrationTimeout / 10

  }

}

class RegisterWithMultipleIdentifiersServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterAll with GuiceOneAppPerSuite {

  val actorSystem: ActorSystem = ActorSystem()

  override def afterAll(): Unit = {
    actorSystem.terminate()
    super.afterAll()
  }

  "RegisterWithMultipleIdentifiersService" should {

    "handle an initial submission with neither registration status nor timestamp in the journey data" in new Setup {

      givenExistingJourneyData(registrationStatus = None, timestamp = None)
      givenStartNewRegistration()
      givenRegisterLimitedCompanySuccess()
      givenUpdateRegistrationStatusSuccess()

      val result: RegistrationStatus = await(callRegister())

      result mustBe Registered(testSafeId)
    }

    "handle an initial submission following a failed matching attempt" in new Setup {

      givenExistingJourneyData(registrationStatus = Some(RegistrationNotCalled), timestamp = None)
      givenStartNewRegistration()
      givenRegisterLimitedCompanySuccess()
      givenUpdateRegistrationStatusSuccess()

      val result: RegistrationStatus = await(callRegister())

      result mustBe Registered(testSafeId)
    }

    "handle a repeat submission where the registration does not complete in the timeout" when {

      "the repeat submission arrives 100ms after the first submission" in new Setup {

        givenExistingJourneyData(registrationStatus = None, timestamp = Some(now))
        givenStartNewRegistration()
        givenRegisterLimitedCompanySuccess()
        givenUpdateRegistrationStatusSuccess()

        Thread.sleep(100)

        val result: RegistrationStatus = Await.result(callRegister(), 2500.millis)

        result mustBe Registered(testSafeId)

        verify(mockJourneyDataService, times(11)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))
      }

      "the repeat submission arrives 1900ms after the first submission (i.e. just before the timeout)" in new Setup {

        givenStartNewRegistration()
        givenRegisterLimitedCompanySuccess()
        givenUpdateRegistrationStatusSuccess()
        givenExistingJourneyData(registrationStatus = None, timestamp = Some(now))

        Thread.sleep(1900)

        val result: RegistrationStatus = Await.result(callRegister(), 2500.millis)

        result mustBe Registered(testSafeId)

        verify(mockJourneyDataService, times(3)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))

      }

    }

    "handle a repeat submission" when {

      "registration completes within the timeout" in new Setup {

        when(mockJourneyDataService.retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))).thenReturn(
          Future.successful((None, Some(now))),
          Future.successful((None, Some(now))),
          Future.successful((None, Some(now))),
          Future.successful((None, Some(now))),
          Future.successful((Some(Registered(testSafeId)), Some(now)))
        )

        Thread.sleep(100)

        val result: RegistrationStatus = Await.result(callRegister(), 2500.millis)

        result mustBe Registered(testSafeId)

        verify(mockJourneyDataService, times(5)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))
      }

      "the repeat submission arrives after the timeout" in new Setup {

        givenExistingJourneyData(registrationStatus = None, timestamp = Some(now))
        givenStartNewRegistration()
        givenRegisterLimitedCompanySuccess()
        givenUpdateRegistrationStatusSuccess()

        Thread.sleep(2250)

        val result: RegistrationStatus = Await.result(callRegister(), 2500.millis)

        result mustBe Registered(testSafeId)

        verify(mockJourneyDataService, times(1)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))
      }

      "an illegal registration state is detected" in new Setup {

        givenStartNewRegistration()
        givenRegisterLimitedCompanySuccess()
        givenUpdateRegistrationStatusSuccess()

        when(mockJourneyDataService.retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))).thenReturn(
          Future.successful((None, Some(now))),
          Future.successful((None, Some(now))),
          Future.successful((Some(Registered(testSafeId)), None))
        )

        Thread.sleep(100)

        val result: RegistrationStatus = Await.result(callRegister(), 2500.millis)

        result mustBe Registered(testSafeId)

        verify(mockJourneyDataService, times(3)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))

      }
    }

    "raise an error when" when {

      "the registration status is defined as success, but the timestamp is not defined" in new Setup {

        givenExistingJourneyData(registrationStatus = Some(Registered(testSafeId)), timestamp = None)

        val result: Either[Exception, RegistrationStatus] =
          try {
            Right(await(callRegister()))
          } catch {
            case ex: Exception => Left(ex)
          }

        result match {
          case Right(_) => fail("Call to method register should have raised an exception")
          case Left(ex) =>
            ex.isInstanceOf[IllegalStateException] mustBe true
            ex.getMessage mustBe s"[VER-5038] Registration status is defined as success, but registration timeout is not defined"
        }

        verify(mockJourneyDataService, times(1)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))
      }

      "the registration status is defined as failed, but the timestamp is not defined" in new Setup {

        givenExistingJourneyData(registrationStatus = Some(RegistrationFailed(Some(failures))), timestamp = None)

        val result: Either[Exception, RegistrationStatus] =
          try {
            Right(await(callRegister()))
          } catch {
            case ex: Exception => Left(ex)
          }

        result match {
          case Right(_) => fail("Call to method register should have raised an exception")
          case Left(ex) =>
            ex.isInstanceOf[IllegalStateException] mustBe true
            ex.getMessage mustBe s"[VER-5038] Registration status is defined as failed, but registration timeout is not defined"
        }

        verify(mockJourneyDataService, times(1)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))
      }

    }

    "handle a repeat submission when the initial submission has already completed" when {

      "the registration timeout has not expired" in new Setup {

        givenExistingJourneyData(registrationStatus = Some(Registered(testSafeId)), timestamp = Some(now))

        Thread.sleep(1000L)

        val result: RegistrationStatus = Await.result(callRegister(), 2500.millis)

        result mustBe Registered(testSafeId)

        verify(mockJourneyDataService, times(1)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))
      }

      "the registration timeout has expired" in new Setup {

        givenExistingJourneyData(registrationStatus = Some(Registered(testSafeId)), timestamp = Some(now))
        givenStartNewRegistration()
        givenRegisterLimitedCompanySuccess()
        givenUpdateRegistrationStatusSuccess()

        Thread.sleep(2500)

        val result: RegistrationStatus = await(callRegister())

        result mustBe Registered(testSafeId)

        verify(mockJourneyDataService, times(1)).retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId))
      }
    }

  }

  class Setup {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val config: Configuration = app.injector.instanceOf[Configuration]
    val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]

    val testAppConfig: TestAppConfig = new TestAppConfig(config, servicesConfig)

    val mockJourneyDataService: JourneyDataService = mock[JourneyDataService]
    val mockRegisterWithMultipleIdentifiersConnector: RegisterWithMultipleIdentifiersConnector = mock[RegisterWithMultipleIdentifiersConnector]

    val testService: RegisterWithMultipleIdentifiersService = new RegisterWithMultipleIdentifiersService(
      actorSystem,
      testAppConfig,
      mockJourneyDataService,
      mockRegisterWithMultipleIdentifiersConnector
    )

    val now: Instant = OffsetDateTime.now(ZoneOffset.UTC).toInstant

    val testJourneyId: String = "testJourneyId"
    val testInternalId: String = "testInternalId"
    val testCompanyNumber: String = "01234567"
    val testCtUtr: String = "1234567890"
    val testRegime: String = "VATC"
    val testSafeId: String = "testSafeId"

    val invalidPayloadCode: String = "INVALID_PAYLOAD"
    val invalidPayloadReason: String = "Request has not passed validation. Invalid payload."

    val failures: Array[Failure] = Array(Failure(invalidPayloadCode, invalidPayloadReason))

    // --- helpers to remove duplication ---

    def givenExistingJourneyData(registrationStatus: Option[RegistrationStatus], timestamp: Option[Instant]): Unit =
      when(mockJourneyDataService.retrieveRegistrationStatusAndTimestamp(eqTo(testJourneyId), eqTo(testInternalId)))
        .thenReturn(Future.successful((registrationStatus, timestamp)))

    def givenStartNewRegistration(): Unit =
      when(mockJourneyDataService.updateRegistrationTimestamp(eqTo(testJourneyId), eqTo(testInternalId), any[Instant]))
        .thenReturn(Future(true))

    def givenRegisterLimitedCompanySuccess(): Unit =
      when(
        mockRegisterWithMultipleIdentifiersConnector
          .registerLimitedCompany(eqTo(testCompanyNumber), eqTo(testCtUtr), eqTo(testRegime))(using any[HeaderCarrier])
      ).thenReturn(Future.successful(Registered(testSafeId)))

    def givenUpdateRegistrationStatusSuccess(): Unit =
      when(
        mockJourneyDataService.updateRegistrationStatus(eqTo(testJourneyId), eqTo(testInternalId), eqTo(Registered(testSafeId)))
      ).thenReturn(Future(true))

    def callRegister(): Future[RegistrationStatus] =
      testService.register(
        testJourneyId,
        testInternalId,
        testCompanyNumber,
        testCtUtr,
        testRegime,
        testService.registerLimitedCompany
      )
  }

}
