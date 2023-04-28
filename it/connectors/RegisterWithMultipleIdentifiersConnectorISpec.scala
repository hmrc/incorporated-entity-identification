/*
 * Copyright 2023 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status.BAD_REQUEST
import play.api.test.Helpers._
import stubs.{AuthStub, RegisterWithMultipleIdentifiersStub}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incorporatedentityidentification.connectors.RegisterWithMultipleIdentifiersConnector
import uk.gov.hmrc.incorporatedentityidentification.httpparsers.RegisterWithMultipleIdentifiersHttpParser.{Failures, RegisterWithMultipleIdentifiersFailure, RegisterWithMultipleIdentifiersSuccess}
import uk.gov.hmrc.incorporatedentityidentification.featureswitch.core.config.{DesStub, FeatureSwitching}
import utils.ComponentSpecHelper

import java.util.UUID

class RegisterWithMultipleIdentifiersConnectorISpec extends ComponentSpecHelper with AuthStub with RegisterWithMultipleIdentifiersStub with FeatureSwitching {
  lazy val connector: RegisterWithMultipleIdentifiersConnector = app.injector.instanceOf[RegisterWithMultipleIdentifiersConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "registerLimitedCompany" should {
    "return OK with status Registered and the SafeId" when {
      "the Registration was a success on the Register API" in {
        val safeId = UUID.randomUUID().toString
        val regime = "VATC"
        val companyNumber = "001"
        val ctutr = "123456"

        wireMockServer.stubFor(
          WireMock.post(urlPathMatching("/cross-regime/register/GRS.*"))
            .willReturn(okJson(s"""
              {
                "identification": [{
                  "idType": "SAFEID",
                  "idValue": "$safeId"
                }]
              }""").withHeader("Content-Type", "application/json")))

        val result = connector.registerLimitedCompany(companyNumber, ctutr, regime)

        await(result) mustBe RegisterWithMultipleIdentifiersSuccess(safeId)
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/cross-regime/register/GRS"))
          .withQueryParam("grsRegime", equalTo(regime))
          .withRequestBody(equalToJson(s"""
            {
              "company": {
                "crn": "$companyNumber",
                "ctutr": "$ctutr"
              }
            }""")))
      }
    }

    "return OK with status Registered and the SafeId" when {
      "the Registration was a success on the Register API stub" when {
        s"the $DesStub feature switch is enabled" in {
          val safeId = UUID.randomUUID().toString
          val regime = "VATC"
          val companyNumber = "001"
          val ctutr = "123456"

          wireMockServer.stubFor(
            WireMock.post(urlPathMatching("/stubbed-url/cross-regime/register/GRS.*"))
              .willReturn(okJson(
                s"""
                {
                  "identification": [{
                    "idType": "SAFEID",
                    "idValue": "$safeId"
                  }]
                }""").withHeader("Content-Type", "application/json")))

          enable(DesStub)
          val result = connector.registerLimitedCompany(companyNumber, ctutr, regime)
          disable(DesStub)

          await(result) mustBe RegisterWithMultipleIdentifiersSuccess(safeId)
          wireMockServer.verify(postRequestedFor(urlPathEqualTo("/stubbed-url/cross-regime/register/GRS"))
            .withQueryParam("grsRegime", equalTo(regime))
            .withRequestBody(equalToJson(s"""
              {
                "company": {
                  "crn": "$companyNumber",
                  "ctutr": "$ctutr"
                }
              }""")))
        }
      }
    }

    "return a failure with the response failure body" when {
      "the Registration was a failure on the Register API stub" in {
        val regime = "VATC"
        val companyNumber = "001"
        val ctutr = "123456"

        wireMockServer.stubFor(
          WireMock.post(urlPathMatching("/cross-regime/register/GRS.*"))
            .willReturn(badRequest().withBody(s"""
              {
                "code": "INVALID_PAYLOAD",
                "reason": "Request has not passed validation. Invalid payload."
              }""").withHeader("Content-Type", "application/json")))

        val result = connector.registerLimitedCompany(companyNumber, ctutr, regime)

        await(result) match {
          case RegisterWithMultipleIdentifiersFailure(status, failures) =>
            status mustBe BAD_REQUEST
            failures.head mustBe Failures("INVALID_PAYLOAD", "Request has not passed validation. Invalid payload.")
          case _ => fail("test returned an invalid registration result")
        }
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/cross-regime/register/GRS"))
          .withQueryParam("grsRegime", equalTo(regime))
          .withRequestBody(equalToJson(s"""
            {
              "company": {
                "crn": "$companyNumber",
                "ctutr": "$ctutr"
              }
            }""")))
      }

      "multiple failures have been returned" in {
        val regime = "VATC"
        val companyNumber = "001"
        val ctutr = "123456"

        wireMockServer.stubFor(
          WireMock.post(urlPathMatching("/cross-regime/register/GRS.*"))
            .willReturn(badRequest().withBody(s"""
              {
                "failures": [
                  {
                    "code": "INVALID_PAYLOAD",
                    "reason": "Request has not passed validation. Invalid payload."
                  },
                  {
                    "code": "INVALID_REGIME",
                    "reason": "Request has not passed validation.  Invalid regime."
                  }]
              }""").withHeader("Content-Type", "application/json")))

        val result = connector.registerLimitedCompany(companyNumber, ctutr, regime)

        await(result) match {
          case RegisterWithMultipleIdentifiersFailure(status, failures) =>
            status mustBe BAD_REQUEST
            failures mustBe Array(
              Failures("INVALID_PAYLOAD", "Request has not passed validation. Invalid payload."),
              Failures("INVALID_REGIME", "Request has not passed validation.  Invalid regime."))
          case _ => fail("test returned an invalid registration result")
        }
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/cross-regime/register/GRS"))
          .withQueryParam("grsRegime", equalTo(regime))
          .withRequestBody(equalToJson(s"""
            {
              "company": {
                "crn": "$companyNumber",
                "ctutr": "$ctutr"
              }
            }""")))
      }
    }
  }

  "registerRegisteredSociety" should {
    "return OK with status Registered and the SafeId" when {
      "the Registration was a success on the Register API" in {
        val safeId = UUID.randomUUID().toString
        val regime = "VATC"
        val companyNumber = "001"
        val ctutr = "123456"

        wireMockServer.stubFor(
          WireMock.post(urlPathMatching(s"/cross-regime/register/GRS.*"))
            .willReturn(okJson(s"""
              {
                "identification": [{
                  "idType": "SAFEID",
                  "idValue": "$safeId"
                }]
              }""").withHeader("Content-Type", "application/json")))

        val result = connector.registerRegisteredSociety(companyNumber, ctutr, regime)

        await(result) mustBe RegisterWithMultipleIdentifiersSuccess(safeId)
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/cross-regime/register/GRS"))
          .withQueryParam("grsRegime", equalTo(regime))
          .withRequestBody(equalToJson(
            s"""
            {
              "registeredSociety": {
                "crn": "$companyNumber",
                "ctutr": "$ctutr"
              }
            }""")))
      }
    }

    "return OK with status Registered and the SafeId" when {
      "the Registration was a success on the Register API stub" when {
        s"the $DesStub feature switch is enabled" in {
          val safeId = UUID.randomUUID().toString
          val regime = "VATC"
          val companyNumber = "001"
          val ctutr = "123456"

          wireMockServer.stubFor(
            WireMock.post(urlPathMatching(s"/stubbed-url/cross-regime/register/GRS.*"))
              .willReturn(okJson(s"""
                {
                  "identification": [{
                    "idType": "SAFEID",
                    "idValue": "$safeId"
                  }]
                }""").withHeader("Content-Type", "application/json")))

          enable(DesStub)
          val result = connector.registerRegisteredSociety(companyNumber, ctutr, regime)
          disable(DesStub)

          await(result) mustBe RegisterWithMultipleIdentifiersSuccess(safeId)
          wireMockServer.verify(postRequestedFor(urlPathEqualTo("/stubbed-url/cross-regime/register/GRS"))
            .withQueryParam("grsRegime", equalTo(regime))
            .withRequestBody(equalToJson(s"""
              {
                "registeredSociety": {
                  "crn": "$companyNumber",
                  "ctutr": "$ctutr"
                }
              }""")))
        }
      }
    }

    "return a failure with the response failure body" when {
      "the Registration was a failure on the Register API stub" in {
        val regime = "VATC"
        val companyNumber = "001"
        val ctutr = "123456"

        wireMockServer.stubFor(
          WireMock.post(urlPathMatching(s"/cross-regime/register/GRS.*"))
            .willReturn(badRequest().withBody(s"""
              {
                "code": "INVALID_PAYLOAD",
                "reason": "Request has not passed validation. Invalid payload."
              }""").withHeader("Content-Type", "application/json")))

        val result = connector.registerRegisteredSociety(companyNumber, ctutr, regime)

        await(result) match {
          case RegisterWithMultipleIdentifiersFailure(status, failures) =>
            status mustBe BAD_REQUEST
            failures.head mustBe Failures("INVALID_PAYLOAD", "Request has not passed validation. Invalid payload.")
          case _ => fail("test returned an invalid registration result")
        }
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/cross-regime/register/GRS"))
          .withQueryParam("grsRegime", equalTo(regime))
          .withRequestBody(equalToJson(s"""
            {
              "registeredSociety": {
                "crn": "$companyNumber",
                "ctutr": "$ctutr"
              }
            }""")))
      }

      "multiple failures have been returned" in {
        val regime = "VATC"
        val companyNumber = "001"
        val ctutr = "123456"

        wireMockServer.stubFor(
          WireMock.post(urlPathMatching("/cross-regime/register/GRS.*"))
            .willReturn(badRequest().withBody(s"""
              {
                "failures": [
                  {
                    "code": "INVALID_PAYLOAD",
                    "reason": "Request has not passed validation. Invalid payload."
                  },
                  {
                    "code": "INVALID_REGIME",
                    "reason": "Request has not passed validation.  Invalid regime."
                  }]
              }""").withHeader("Content-Type", "application/json")))

        val result = connector.registerRegisteredSociety(companyNumber, ctutr, regime)
        await(result) match {
          case RegisterWithMultipleIdentifiersFailure(status, failures) =>
            status mustBe BAD_REQUEST
            failures mustBe Array(
              Failures("INVALID_PAYLOAD", "Request has not passed validation. Invalid payload."),
              Failures("INVALID_REGIME", "Request has not passed validation.  Invalid regime."))
          case _ => fail("test returned an invalid registration result")
        }
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/cross-regime/register/GRS"))
          .withQueryParam("grsRegime", equalTo(regime))
          .withRequestBody(equalToJson(s"""
            {
              "registeredSociety": {
                "crn": "$companyNumber",
                "ctutr": "$ctutr"
              }
            }""")))
      }
    }
  }
}
