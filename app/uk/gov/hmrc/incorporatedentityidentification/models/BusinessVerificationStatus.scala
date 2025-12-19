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

package uk.gov.hmrc.incorporatedentityidentification.models

import play.api.libs.json.*

sealed trait BusinessVerificationStatus

case object BusinessVerificationPass extends BusinessVerificationStatus

case object BusinessVerificationFail extends BusinessVerificationStatus

case object BusinessVerificationNotEnoughInformationToChallenge extends BusinessVerificationStatus

case object BusinessVerificationNotEnoughInformationToCallBV extends BusinessVerificationStatus

case object CtEnrolled extends BusinessVerificationStatus

object BusinessVerificationStatus {

  val businessVerificationPassKey = "PASS"
  val businessVerificationFailKey = "FAIL"
  val businessVerificationNotEnoughInfoToChallengeKey = "NOT_ENOUGH_INFORMATION_TO_CHALLENGE"
  val businessVerificationNotEnoughInfoToCallBVKey = "NOT_ENOUGH_INFORMATION_TO_CALL_BV"
  val businessVerificationCtEnrolledKey = "CT_ENROLLED"
  val businessVerificationStatusKey = "verificationStatus"

  implicit val reads: Reads[BusinessVerificationStatus] = (json: JsValue) =>
    (json \ businessVerificationStatusKey).validate[String].collect(JsonValidationError("Invalid business validation state")) {
      case `businessVerificationPassKey`                     => BusinessVerificationPass
      case `businessVerificationFailKey`                     => BusinessVerificationFail
      case `businessVerificationNotEnoughInfoToChallengeKey` => BusinessVerificationNotEnoughInformationToChallenge
      case `businessVerificationNotEnoughInfoToCallBVKey`    => BusinessVerificationNotEnoughInformationToCallBV
      case `businessVerificationCtEnrolledKey`               => CtEnrolled
    }

}
