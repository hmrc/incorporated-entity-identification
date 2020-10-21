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

import assets.TestConstants.{testCompanyNumber, testCtutr}
import play.api.libs.json.Json
import play.api.test.Helpers._
import stubs.{AuthStub, GetCtReferenceStub}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incorporatedentityidentification.connectors.GetCtReferenceConnector
import utils.ComponentSpecHelper

class GetCtReferenceConnectorISpec extends ComponentSpecHelper with AuthStub with GetCtReferenceStub{

  lazy val connector: GetCtReferenceConnector = app.injector.instanceOf[GetCtReferenceConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "getCtReference" should {
    "return the CT reference" in {
      stubGetCtReference(testCompanyNumber)(status = OK, body = Json.obj("CTUTR" -> testCtutr))
      val res = connector.getCtReference(testCompanyNumber)

      await(res) mustBe Some(testCtutr)
    }
  }

  "getCtReference" should {
    "return not found" in {
      stubGetCtReference(testCompanyNumber)(status = NOT_FOUND)
      val res = connector.getCtReference("000000000")

      await(res) mustBe None
    }
  }

}