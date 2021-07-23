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

package stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, Json}
import utils.WireMockMethods

trait RegisterWithMultipleIdentifiersStub extends WireMockMethods {

  private def registerResponseSuccessBody(safeId: String): JsObject =
    Json.obj(
      "identification" -> Json.arr(
        Json.obj(
          "idType" -> "SAFEID",
          "idValue" -> safeId
        )
      )
    )

  private def registerResponseFailureBody(): JsObject =
    Json.obj(
      "code" -> "INVALID_PAYLOAD",
      "reason" -> "Request has not passed validation. Invalid Payload."
    )


  def stubRegisterWithMultipleIdentifiersSuccess(postBody: JsObject)(status: Int, safeId: String): StubMapping =

    when(method = POST, uri = "/cross-regime/register/VATC", postBody)
      .thenReturn(
        status = status,
        body = registerResponseSuccessBody(safeId)
      )

  def stubRegisterWithMultipleIdentifiersFailure(postBody: JsObject)(status: Int): StubMapping =
    when(method = POST, uri = "/cross-regime/register/VATC", postBody)
      .thenReturn(
        status = status,
        body = registerResponseFailureBody()
      )

}
