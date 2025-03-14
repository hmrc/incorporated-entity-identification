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

package uk.gov.hmrc.incorporatedentityidentification.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.internalId
import uk.gov.hmrc.incorporatedentityidentification.models._
import uk.gov.hmrc.incorporatedentityidentification.models.{Registered, RegistrationFailed}
import uk.gov.hmrc.incorporatedentityidentification.models.RegistrationStatus.RegistrationFailuresFormat
import uk.gov.hmrc.incorporatedentityidentification.models.error.DataAccessException
import uk.gov.hmrc.incorporatedentityidentification.services.{JourneyDataService, RegisterWithMultipleIdentifiersService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterBusinessEntityController @Inject() (cc: ControllerComponents,
                                                  journeyDataService: JourneyDataService,
                                                  registerWithMultipleIdentifiersService: RegisterWithMultipleIdentifiersService,
                                                  val authConnector: AuthConnector
                                                 )(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions {

  def registerLimitedCompany(): Action[JsValue] = Action.async(parse.json) { implicit request =>

    authorised().retrieve(internalId) {
      case Some(internalId) =>
        val optJourneyId: Option[String] = (request.body \ "journeyId").asOpt[String]
        val optBusinessVerificationCheck: Option[Boolean] = (request.body \ "businessVerificationCheck").asOpt[Boolean]
        val optRegime = (request.body \ "regime").asOpt[String]

        (optJourneyId, optBusinessVerificationCheck, optRegime) match {
          case (Some(journeyId), Some(businessVerificationCheck), Some(regime)) =>
            register(journeyId, internalId, businessVerificationCheck, regime, registerWithMultipleIdentifiersService.registerLimitedCompany)
          case _ => Future.successful(BadRequest("Invalid parameter list"))
        }
      case None => Future.successful(Unauthorized("InternalId not found"))
    }
  }

  def registerRegisteredSociety(): Action[JsValue] = Action.async(parse.json) { implicit request =>

    authorised().retrieve(internalId) {
      case Some(internalId) =>
        val optJourneyId: Option[String] = (request.body \ "journeyId").asOpt[String]
        val optBusinessVerificationCheck: Option[Boolean] = (request.body \ "businessVerificationCheck").asOpt[Boolean]
        val optRegime = (request.body \ "regime").asOpt[String]

        (optJourneyId, optBusinessVerificationCheck, optRegime) match {
          case (Some(journeyId), Some(businessVerificationCheck), Some(regime)) =>
            register(journeyId, internalId, businessVerificationCheck, regime, registerWithMultipleIdentifiersService.registerRegisteredSociety)
          case _ => Future.successful(BadRequest("Invalid parameter list"))
        }
      case None => Future.successful(Unauthorized("InternalId not found"))
    }
  }

  private def register(journeyId: String,
                       authInternalId: String,
                       businessVerificationCheck: Boolean,
                       regime: String,
                       registrationFunction: (String, String, String) => Future[RegistrationStatus]
                      ): Future[Result] = {
    (for {
      shouldRegister <- journeyDataService.retrieveBusinessVerificationStatus(journeyId, authInternalId).map {
                          case Some(BusinessVerificationPass | CtEnrolled) => true
                          case Some(
                                BusinessVerificationNotEnoughInformationToChallenge |
                                BusinessVerificationNotEnoughInformationToCallBV | BusinessVerificationFail
                              ) =>
                            false
                          case None if !businessVerificationCheck => true
                          case None =>
                            throw DataAccessException(s"Missing business verification state in database for journey $journeyId")
                        }
      result <- if (shouldRegister) for {
                  (optCompanyProfile, optCtUtr) <- journeyDataService.retrieveCompanyProfileAndCtUtr(journeyId, authInternalId)
                  result <- (optCompanyProfile, optCtUtr) match {
                              case (Some(companyProfile), Some(ctUtr)) =>
                                registerWithMultipleIdentifiersService
                                  .register(journeyId, authInternalId, companyProfile.companyNumber, ctUtr, regime, registrationFunction)
                                  .map {
                                    case Registered(safeId) =>
                                      Ok(
                                        Json.obj(
                                          "registration" -> Json.obj("registrationStatus" -> "REGISTERED", "registeredBusinessPartnerId" -> safeId)
                                        )
                                      )
                                    case RegistrationFailed(registrationFailures) =>
                                      registrationFailures match {
                                        case Some(body) =>
                                          Ok(Json.obj("registration" -> Json.obj("registrationStatus" -> "REGISTRATION_FAILED", "failures" -> body)))
                                        case None =>
                                          Ok(
                                            Json.obj(
                                              "registration" -> Json.obj("registrationStatus" -> "REGISTRATION_FAILED", "failures" -> Json.obj())
                                            )
                                          )
                                      }
                                    case RegistrationNotCalled =>
                                      throw new IllegalStateException(s"Unexpected registration not called result returned in journey $journeyId")
                                  }
                              case _ => throw DataAccessException(s"Missing required data for registration in database for journey $journeyId")
                            }
                } yield result
                else {
                  journeyDataService.updateRegistrationStatus(journeyId, authInternalId, RegistrationNotCalled).map { _ =>
                    Ok(Json.obj("registration" -> Json.obj("registrationStatus" -> "REGISTRATION_NOT_CALLED")))
                  }
                }
    } yield result).recover {
      case dataAccessException: DataAccessException     => InternalServerError(dataAccessException.msg)
      case illegalStateException: IllegalStateException => InternalServerError(illegalStateException.getMessage)
    }
  }
}
