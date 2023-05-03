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

package uk.gov.hmrc.incorporatedentityidentification.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.incorporatedentityidentification.connectors.GetCtReferenceConnector
import uk.gov.hmrc.incorporatedentityidentification.models.{DetailsMatched, DetailsMismatched, DetailsNotFound, DetailsDownstreamError, IncorporatedEntityDetailsValidationResult}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidateIncorporatedEntityDetailsService @Inject()(getCtReferenceConnector: GetCtReferenceConnector)(implicit ec: ExecutionContext) {

  def validateDetails(companyNumber: String, optCtUtr: Option[String])(implicit hc: HeaderCarrier): Future[IncorporatedEntityDetailsValidationResult] = {
    getCtReferenceConnector.getCtReference(companyNumber).map {
      case Right(retrievedCtUtr) => optCtUtr match {
        case Some(`retrievedCtUtr`) => DetailsMatched
        case Some(_) | None         => DetailsMismatched
      }
      case Left(error: NotFoundException) => DetailsNotFound(error.getMessage)
      case Left(error)                    => DetailsDownstreamError(error.getMessage)
    }
  }

}
