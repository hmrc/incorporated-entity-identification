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

package connectors

import assets.TestConstants.{testCompanyNumber, testCtutr, testInternalId, testSafeId}
import play.api.libs.json.Json
import play.api.test.Helpers._
import stubs.{AuthStub, RegisterWithMultipleIdentifiersStub}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incorporatedentityidentification.connectors.RegisterWithMultipleIdentifiersConnector
import uk.gov.hmrc.incorporatedentityidentification.connectors.RegisterWithMultipleIdentifiersHttpParser.RegisterWithMultipleIdentifiersSuccess
import utils.ComponentSpecHelper

class RegisterWithMultipleIdentifiersConnectorISpec extends ComponentSpecHelper with AuthStub with RegisterWithMultipleIdentifiersStub {
  lazy val connector: RegisterWithMultipleIdentifiersConnector = app.injector.instanceOf[RegisterWithMultipleIdentifiersConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  "registerWithMultipleIdentifiers" should {
    "return OK with status Registered and the SafeId" when {
      "the Registration was a success" in {
        stubRegisterWithMultipleIdentifiersSuccess(testCompanyNumber, testCtutr)(OK, testSafeId)
        val result = connector.register(testCompanyNumber, testCtutr)
      await(result) mustBe(RegisterWithMultipleIdentifiersSuccess(testSafeId))

      }
    }
  }
}
