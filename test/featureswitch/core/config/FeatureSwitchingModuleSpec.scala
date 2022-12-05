/*
 * Copyright 2022 HM Revenue & Customs
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

package featureswitch.core.config

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import uk.gov.hmrc.incorporatedentityidentification.featureswitch.core.config.{DesStub, FeatureSwitchingModule, StubGetCtReference}

class FeatureSwitchingModuleSpec extends AnyWordSpec with Matchers {

  object TestFeatureSwitchingModule extends FeatureSwitchingModule()

  "FeatureSwitchingModule" should {

    "contain the feature switches for the DES and Get Ct reference stubs" in {

      TestFeatureSwitchingModule.switches mustBe Seq(StubGetCtReference, DesStub)

    }

    "be able to access the Get Ct reference stub feature switch" in {

      TestFeatureSwitchingModule("feature-switch.ct-reference-stub") mustBe StubGetCtReference

    }

    "be able to access the DES stub feature switch" in {

      TestFeatureSwitchingModule("feature-switch.des-stub") mustBe DesStub

    }

    "raise an IllegalArgumentException when asked to access an unknown feature switch" in {

      try {
        TestFeatureSwitchingModule("unknown")
        fail("FeatureSwitchingModule must throw an IllegalArgumentException if the feature switch is not known")
      } catch {
        case _: IllegalArgumentException => succeed
      }

    }

  }

}
