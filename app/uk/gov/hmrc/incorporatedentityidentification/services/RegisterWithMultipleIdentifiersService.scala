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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incorporatedentityidentification.connectors.RegisterWithMultipleIdentifiersConnector
import uk.gov.hmrc.incorporatedentityidentification.httpparsers.RegisterWithMultipleIdentifiersHttpParser.RegisterWithMultipleIdentifiersResult

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RegisterWithMultipleIdentifiersService @Inject() (registerWithMultipleIdentifiersConnector: RegisterWithMultipleIdentifiersConnector) {

  def registerLimitedCompany(companyNumber: String, ctutr: String, regime: String)(implicit
    hc: HeaderCarrier
  ): Future[RegisterWithMultipleIdentifiersResult] =
    registerWithMultipleIdentifiersConnector.registerLimitedCompany(companyNumber, ctutr, regime)

  def registerRegisteredSociety(companyNumber: String, ctutr: String, regime: String)(implicit
    hc: HeaderCarrier
  ): Future[RegisterWithMultipleIdentifiersResult] =
    registerWithMultipleIdentifiersConnector.registerRegisteredSociety(companyNumber, ctutr, regime)

}
