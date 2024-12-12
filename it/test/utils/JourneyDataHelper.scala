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

import play.api.libs.json.{JsObject, Json}
import assets.TestConstants._

object JourneyDataHelper {

  private val companyProfile: JsObject = Json.obj(
    "companyProfile" -> Json.obj(
      "companyName" -> testCompanyName,
      "companyNumber" -> testCompanyNumber,
      "dateOfIncorporation" -> "2020-01-01",
      "unsanitisedCHROAddress" -> Json.obj(
      "address_line_1" -> "testLine1",
        "address_line_2" -> "test town",
        "care_of" -> "test name",
        "country" -> "United Kingdom",
        "locality" -> "test city",
        "po_box" -> "123",
        "postal_code" -> "AA11AA",
        "premises" -> "1",
        "region" -> "test region"
      )
    )
  )

  private def businessVerificationStatus(status: String): JsObject = Json.obj(
    "businessVerification" -> Json.obj(
      "verificationStatus" -> status
    )
  )

  private def ctUtr(utr: String): JsObject = Json.obj(
    "ctutr" -> utr
  )

  def getJourneyDataForRegistration(journeyId: String, authInternalId: String, withCompanyData: Boolean, optBusinessVerificationStatus: Option[String], optCtUtr: Option[String]): JsObject = {

    val coreJourneyObj: JsObject = Json.obj(
      "_id" -> journeyId,
      "authInternalId" -> authInternalId,
      "identifiersMatch" -> "DetailsMatched"
    )

    val companyProfileObj: JsObject = if(withCompanyData) companyProfile else Json.obj()

    val businessVerificationStatusObj: JsObject = optBusinessVerificationStatus match {
      case Some(status) => businessVerificationStatus(status)
      case None => Json.obj()
    }

    val ctUtrObj: JsObject = optCtUtr match {
      case Some(value) => ctUtr(value)
      case None => Json.obj()
    }

    coreJourneyObj ++ companyProfileObj ++ businessVerificationStatusObj ++ ctUtrObj
  }

}
