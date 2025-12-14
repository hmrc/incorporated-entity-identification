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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.EitherValues.*
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.incorporatedentityidentification.connectors.GetCtReferenceConnector
import uk.gov.hmrc.incorporatedentityidentification.featureswitch.core.config.{FeatureSwitching, StubGetCtReference}
import utils.ComponentSpecHelper

class GetCtReferenceConnectorISpec extends ComponentSpecHelper with FeatureSwitching {

  lazy val connector: GetCtReferenceConnector = app.injector.instanceOf[GetCtReferenceConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "returns the Corporate Tax (CT) reference" when {
    "DES returns the CTUTR based on the company number" in {
      val companyNumber = "001"
      val ctutr = "123456"

      wireMockServer.stubFor(
        WireMock
          .get(urlPathMatching(s"/corporation-tax/identifiers/crn/.*"))
          .willReturn(okJson(s"""{"CTUTR": "$ctutr"}""").withHeader("Content-Type", "application/json"))
      )

      val res = connector.getCtReference(companyNumber)

      await(res) mustBe Right(ctutr)
      wireMockServer.verify(getRequestedFor(urlPathEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber")))
    }
  }

  "returns a wrapped exception with further information" when {
    "DES returns 404 for the given company number" in {
      val companyNumber = "001"

      wireMockServer.stubFor(
        WireMock
          .get(urlPathMatching(s"/corporation-tax/identifiers/crn/.*"))
          .willReturn(notFound())
      )

      val response = connector.getCtReference(companyNumber)

      val result = await(response)
      result.left.value mustBe a[NotFoundException]
      result.left.value.message mustBe "HoD has indicated that CT UTR cannot be returned on GET <http://localhost:11111/corporation-tax/identifiers/crn/001>"
      wireMockServer.verify(getRequestedFor(urlPathEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber")))
    }

    "DES returns OK but the body is malformed" in {
      val companyNumber = "001"

      wireMockServer.stubFor(
        WireMock
          .get(urlPathMatching(s"/corporation-tax/identifiers/crn/.*"))
          .willReturn(okJson("""{"unknown": true}"""))
      )

      val result = await(connector.getCtReference(companyNumber))

      result.left.value mustBe a[BadGatewayException]
      result.left.value.message mustBe "HoD returned a malformed JSON on GET <http://localhost:11111/corporation-tax/identifiers/crn/001> errors: List((,List(JsonValidationError(List('CTUTR' is undefined on object. Available keys are 'unknown'),ArraySeq()))))"
      wireMockServer.verify(getRequestedFor(urlPathEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber")))
    }

    "DES returns OK but the body contains an error html page" in {
      val companyNumber = "001"

      wireMockServer.stubFor(
        WireMock
          .get(urlPathMatching(s"/corporation-tax/identifiers/crn/.*"))
          .willReturn(ok("<html></html>"))
      )

      val result = await(connector.getCtReference(companyNumber))

      result.left.value mustBe a[BadGatewayException]
      result.left.value.message mustBe "HoD returned a malformed JSON on GET <http://localhost:11111/corporation-tax/identifiers/crn/001> errors: Unexpected character ('<' (code 60)): ex"
      wireMockServer.verify(getRequestedFor(urlPathEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber")))
    }

    "DES returns 502 and a html page" in {
      val companyNumber = "001"

      wireMockServer.stubFor(
        WireMock
          .get(urlPathMatching(s"/corporation-tax/identifiers/crn/.*"))
          .willReturn(aResponse.withStatus(502).withBody("<html></html>"))
      )

      val result = await(connector.getCtReference(companyNumber))

      result.left.value mustBe a[BadGatewayException]
      result.left.value.message mustBe "HoD returned status code <502> on GET <http://localhost:11111/corporation-tax/identifiers/crn/001>"
      wireMockServer.verify(getRequestedFor(urlPathEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber")))
    }
  }

  s"the $StubGetCtReference feature switch is enabled" should {
    "call the stubbed Get CT Reference API" in {
      val companyNumber = "001"
      val ctutr = "123456"

      wireMockServer.stubFor(
        WireMock
          .get(urlPathMatching(s"/stubbed-url/corporation-tax/identifiers/crn/.*"))
          .willReturn(okJson(s"""{"CTUTR": "$ctutr"}""").withHeader("Content-Type", "application/json"))
      )

      enable(StubGetCtReference)
      val res = connector.getCtReference(companyNumber)
      disable(StubGetCtReference)

      await(res) mustBe Right(ctutr)
      wireMockServer.verify(getRequestedFor(urlPathEqualTo(s"/stubbed-url/corporation-tax/identifiers/crn/$companyNumber")))
    }
  }
}
