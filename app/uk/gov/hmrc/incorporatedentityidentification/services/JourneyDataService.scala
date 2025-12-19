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

import play.api.libs.json.*
import uk.gov.hmrc.incorporatedentityidentification.models.BusinessVerificationStatus.reads
import uk.gov.hmrc.incorporatedentityidentification.models.RegistrationStatus.format
import uk.gov.hmrc.incorporatedentityidentification.models.error.DataAccessException
import uk.gov.hmrc.incorporatedentityidentification.models.{BusinessVerificationStatus, CompanyProfile, RegistrationStatus}
import uk.gov.hmrc.incorporatedentityidentification.repositories.JourneyDataRepository

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyDataService @Inject() (incorporatedEntityIdentificationRepository: JourneyDataRepository,
                                    journeyIdGenerationService: JourneyIdGenerationService
                                   )(implicit ec: ExecutionContext) {

  def createJourney(authInternalId: String): Future[String] = {
    val journeyId = journeyIdGenerationService.generateJourneyId()
    incorporatedEntityIdentificationRepository.createJourney(journeyId, authInternalId)
  }

  def getJourneyData(journeyId: String, authInternalId: String): Future[Option[JsObject]] = {
    incorporatedEntityIdentificationRepository.getJourneyData(journeyId, authInternalId)
  }

  def getJourneyDataByKey(journeyId: String, dataKey: String, authInternalId: String): Future[Option[JsValue]] = {
    incorporatedEntityIdentificationRepository.getJourneyData(journeyId, authInternalId).map {
      case Some(json) => (json \ dataKey).asOpt[JsValue]
      case None       => None
    }
  }

  def updateJourneyData(journeyId: String, dataKey: String, data: JsValue, authInternalId: String): Future[Boolean] = {
    incorporatedEntityIdentificationRepository.updateJourneyData(journeyId, dataKey, data, authInternalId)
  }

  def removeJourneyDataField(journeyId: String, authInternalId: String, dataKey: String): Future[Boolean] = {
    incorporatedEntityIdentificationRepository.removeJourneyDataField(journeyId, authInternalId, dataKey)
  }

  def removeJourneyData(journeyId: String, authInternalId: String): Future[Boolean] = {
    incorporatedEntityIdentificationRepository.removeJourneyData(journeyId, authInternalId)
  }

  def retrieveBusinessVerificationStatus(journeyId: String, authInternalId: String): Future[Option[BusinessVerificationStatus]] = {

    getJourneyData(journeyId, authInternalId).map {
      case Some(json) =>
        if (json.keys.contains("businessVerification")) {
          (json \ "businessVerification").validate[BusinessVerificationStatus] match {
            case JsSuccess(businessVerificationStatus, _) => Some(businessVerificationStatus)
            case _: JsError => throw DataAccessException(s"Error occurred parsing business verification status for journey $journeyId")
          }
        } else None
      case None => None
    }

  }

  def retrieveCompanyProfileAndCtUtr(journeyId: String, authInternalId: String): Future[(Option[CompanyProfile], Option[String])] = {

    getJourneyData(journeyId, authInternalId).map {
      case Some(json) => {
        val optCompanyProfile: Option[CompanyProfile] =
          if (json.keys.contains("companyProfile")) {
            (json \ "companyProfile").validate[CompanyProfile] match {
              case JsSuccess(companyProfile, _) => Some(companyProfile)
              case _: JsError                   => throw DataAccessException(s"Error occurred parsing company profile for journey $journeyId")
            }
          } else None

        val optCtUtr: Option[String] =
          if (json.keys.contains("ctutr")) {
            (json \ "ctutr").validate[String] match {
              case JsSuccess(ctUtr, _) => Some(ctUtr)
              case _: JsError          => throw DataAccessException(s"Error occurred parsing Ct UTR for journey $journeyId")
            }
          } else None

        (optCompanyProfile, optCtUtr)
      }
      case None => (None, None)
    }

  }

  def updateRegistrationTimestamp(journeyId: String, authInternalId: String, registrationTimestamp: Instant): Future[Boolean] =
    updateJourneyData(journeyId, "registrationTimestamp", Json.obj("$date" -> registrationTimestamp.toEpochMilli), authInternalId)

  def updateRegistrationStatus(journeyId: String, authInternalId: String, registrationStatus: RegistrationStatus): Future[Boolean] =
    updateJourneyData(journeyId, "registration", Json.toJson(registrationStatus), authInternalId)

  def retrieveRegistrationStatusAndTimestamp(journeyId: String, authInternalId: String): Future[(Option[RegistrationStatus], Option[Instant])] = {

    getJourneyData(journeyId, authInternalId).map {
      case Some(journeyData) => (parseRegistrationStatus(journeyId, journeyData), parseRegistrationTimestamp(journeyId, journeyData))
      case None              => (None, None)
    }

  }

  def retrieveRegistrationStatus(journeyId: String, authInternalId: String): Future[Option[RegistrationStatus]] =
    getJourneyData(journeyId, authInternalId).map {
      case Some(journeyData) => parseRegistrationStatus(journeyId, journeyData)
      case None              => None
    }

  def retrieveRegistrationTimestamp(journeyId: String, authInternalId: String): Future[Option[Instant]] =
    getJourneyData(journeyId, authInternalId).map {
      case Some(journeyData) => parseRegistrationTimestamp(journeyId, journeyData)
      case None              => None
    }

  private def parseRegistrationStatus(journeyId: String, journeyData: JsObject): Option[RegistrationStatus] = {

    if (journeyData.keys.contains("registration")) {
      (journeyData \ "registration").validate[RegistrationStatus] match {
        case JsSuccess(registrationStatus, _) => Some(registrationStatus)
        case _: JsError => throw DataAccessException(s"[VER-5038] Error occurred parsing registration status for journey $journeyId")
      }
    } else None

  }

  private def parseRegistrationTimestamp(journeyId: String, journeyData: JsObject): Option[Instant] = {

    if (journeyData.keys.contains("registrationTimestamp")) {
      (journeyData \ "registrationTimestamp" \ """$date""" \ """$numberLong""").validate[String] match {
        case JsSuccess(registrationTimestamp, _) => Some(Instant.ofEpochMilli(registrationTimestamp.toLong))
        case _: JsError => throw DataAccessException(s"[VER-5038] Error occurred parsing registration timestamp for journey $journeyId")
      }
    } else None

  }
}
