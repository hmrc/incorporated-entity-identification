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

package uk.gov.hmrc.incorporatedentityidentification.services

import org.apache.pekko
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incorporatedentityidentification.config.AppConfig
import uk.gov.hmrc.incorporatedentityidentification.connectors.RegisterWithMultipleIdentifiersConnector
import uk.gov.hmrc.incorporatedentityidentification.models.error.{IllegalRegistrationStateError, IncorporatedEntityIdentificationError, RegistrationSubmissionRetryTimedOutError}
import uk.gov.hmrc.incorporatedentityidentification.models.{Registered, RegistrationNotCalled, RegistrationStatus}

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterWithMultipleIdentifiersService @Inject() (actorSystem: ActorSystem,
                                                        appConfig: AppConfig,
                                                        journeyDataService: JourneyDataService,
                                                        registerWithMultipleIdentifiersConnector: RegisterWithMultipleIdentifiersConnector
                                                       )(implicit ec: ExecutionContext)
    extends Logging {

  val registrationRetryTimeout: Long = appConfig.registrationTimeout

  val registrationRetryInterval: Long = appConfig.determineRetryInterval()

  def registerLimitedCompany(companyNumber: String, ctutr: String, regime: String)(implicit
    hc: HeaderCarrier
  ): Future[RegistrationStatus] =
    registerWithMultipleIdentifiersConnector.registerLimitedCompany(companyNumber, ctutr, regime)

  def registerRegisteredSociety(companyNumber: String, ctutr: String, regime: String)(implicit
    hc: HeaderCarrier
  ): Future[RegistrationStatus] =
    registerWithMultipleIdentifiersConnector.registerRegisteredSociety(companyNumber, ctutr, regime)

  def register(journeyId: String,
               authInternalId: String,
               companyNumber: String,
               ctutr: String,
               regime: String,
               registrationFunction: (String, String, String) => Future[RegistrationStatus]
              ): Future[RegistrationStatus] = {

    journeyDataService.retrieveRegistrationStatusAndTimestamp(journeyId, authInternalId).flatMap {
      case (None, None) => handleInitialSubmission(journeyId, authInternalId, companyNumber, ctutr, regime, registrationFunction)
      case (None, Some(registrationTimestamp)) =>
        handleRepeatSubmissionBeforeRegistrationComplete(journeyId,
                                                         authInternalId,
                                                         companyNumber,
                                                         ctutr,
                                                         regime,
                                                         registrationTimestamp,
                                                         registrationFunction
                                                        )
      case (Some(registrationStatus), None) =>
        registrationStatus match {
          case RegistrationNotCalled => handleInitialSubmission(journeyId, authInternalId, companyNumber, ctutr, regime, registrationFunction)
          case other: RegistrationStatus =>
            val status: String = if (other.isInstanceOf[Registered]) "success" else "failed"
            logger.error(s"""[VER-5038] Registration status of "$status", but no timestamp for journey $journeyId""")
            Future
              .failed(new IllegalStateException(s"[VER-5038] Registration status is defined as $status, but registration timeout is not defined"))
        }
      case (Some(registrationStatus), Some(registrationTimestamp)) =>
        handleRepeatSubmissionAfterRegistrationComplete(journeyId,
                                                        authInternalId,
                                                        companyNumber,
                                                        ctutr,
                                                        regime,
                                                        registrationStatus,
                                                        registrationTimestamp,
                                                        registrationFunction
                                                       )

    }

  }

  /** Neither the registration status nor registration timeout are defined so this is the initial submission for registration
    */
  private def handleInitialSubmission(journeyId: String,
                                      authInternalId: String,
                                      companyNumber: String,
                                      ctutr: String,
                                      regime: String,
                                      registrationFunction: (String, String, String) => Future[RegistrationStatus]
                                     ): Future[RegistrationStatus] = {

    logger.info(s"[VER-5038] Handling initial submission for journey $journeyId")

    journeyDataService.updateRegistrationTimestamp(journeyId, authInternalId, createTimestamp()).flatMap { _ =>

      registrationFunction(companyNumber, ctutr, regime).flatMap { registrationStatus =>

        journeyDataService.updateRegistrationStatus(journeyId, authInternalId, registrationStatus).map(_ => registrationStatus)

      }

    }

  }

  private def handleRepeatSubmissionBeforeRegistrationComplete(
    journeyId: String,
    authInternalId: String,
    companyNumber: String,
    ctutr: String,
    regime: String,
    registrationTimestamp: Instant,
    registrationFunction: (String, String, String) => Future[RegistrationStatus]
  )(implicit ec: ExecutionContext): Future[RegistrationStatus] = {

    def retryFunction(): Future[Either[IncorporatedEntityIdentificationError, RegistrationStatus]] = {

      journeyDataService.retrieveRegistrationStatusAndTimestamp(journeyId, authInternalId).flatMap {
        case (Some(registrationStatus), Some(_)) => Future.successful(Right(registrationStatus))
        case (None, Some(timestamp)) =>
          val now: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

          val currentTime = Instant.ofEpochMilli(now.toInstant.toEpochMilli)

          if (currentTime.toEpochMilli - timestamp.toEpochMilli < registrationRetryTimeout) {
            logger.info(s"[VER-5038] Registration status not defined in retry attempt for journey $journeyId")
            Future.failed(new IllegalStateException("[VER-5038] Registration status not found"))
          } else {
            logger.warn(s"[VER-5038] Registration status retry check timed out for journey $journeyId")
            Future.successful(Left(RegistrationSubmissionRetryTimedOutError))
          }
        case _ =>
          logger.error(s"[VER-5038] Unexpected combination of registration status and timeout encountered in journey $journeyId")
          Future.successful(Left(IllegalRegistrationStateError))
      }
    }

    val currentTime: Instant = createTimestamp()

    val timeout: Long = registrationTimestamp.toEpochMilli + registrationRetryTimeout

    logger.warn(
      s"[VER-5038] Repeat submission before registration complete for journey $journeyId. Registration timestamp : $registrationTimestamp and current time : $currentTime"
    )

    if (currentTime.toEpochMilli < timeout) {

      val remainingTime: Long = timeout - currentTime.toEpochMilli

      val retries = (remainingTime / registrationRetryInterval).toInt + 1

      logger.warn(s"[VER-5038] Activating registration status retry mechanism with $retries attempts for journey $journeyId")

      val retrySubmission: Future[Either[IncorporatedEntityIdentificationError, RegistrationStatus]] = pekko.pattern
        .retry(attempt = () => retryFunction(), attempts = retries, delay = registrationRetryInterval.millis)(using ec, actorSystem.scheduler)

      retrySubmission.flatMap {
        case Left(_) =>
          journeyDataService.updateRegistrationTimestamp(journeyId, authInternalId, createTimestamp()).flatMap { _ =>
            registrationFunction(companyNumber, ctutr, regime).flatMap { registrationStatus =>
              journeyDataService.updateRegistrationStatus(journeyId, authInternalId, registrationStatus).map(_ => registrationStatus)
            }
          }
        case Right(registrationStatus) =>
          logger.warn(s"[VER-5038] Registration status found for journey $journeyId")
          Future.successful(registrationStatus)
      }

    } else {
      handleInitialSubmission(journeyId, authInternalId, companyNumber, ctutr, regime, registrationFunction)
    }

  }

  private def handleRepeatSubmissionAfterRegistrationComplete(
    journeyId: String,
    authInternalId: String,
    companyNumber: String,
    ctutr: String,
    regime: String,
    registrationStatus: RegistrationStatus,
    registrationTimestamp: Instant,
    registrationFunction: (String, String, String) => Future[RegistrationStatus]
  ): Future[RegistrationStatus] = {

    val currentIime: Instant = createTimestamp()

    if (currentIime.toEpochMilli - registrationTimestamp.toEpochMilli < registrationRetryTimeout) {

      logger.warn(s"[VER-5038] Registration completed before repeat submission for journey $journeyId")

      Future.successful(registrationStatus)

    } else {

      journeyDataService.updateRegistrationTimestamp(journeyId, authInternalId, createTimestamp()).flatMap { _ =>

        registrationFunction(companyNumber, ctutr, regime).flatMap { registrationStatus =>

          journeyDataService.updateRegistrationStatus(journeyId, authInternalId, registrationStatus).map(_ => registrationStatus)
        }

      }

    }

  }

  private def createTimestamp(): Instant = {

    val now: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

    Instant.ofEpochMilli(now.toInstant.toEpochMilli)
  }

}
