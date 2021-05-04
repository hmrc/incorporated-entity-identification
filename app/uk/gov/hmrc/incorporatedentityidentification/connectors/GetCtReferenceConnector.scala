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

package uk.gov.hmrc.incorporatedentityidentification.connectors

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.incorporatedentityidentification.config.AppConfig
import uk.gov.hmrc.incorporatedentityidentification.httpparsers.GetCtReferenceHttpParser.GetCtReferenceHttpReads

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetCtReferenceConnector @Inject()(http: HttpClient,
                                        appConfig: AppConfig
                                       )(implicit ec: ExecutionContext) {

  def getCtReference(companyNumber: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val extraHeaders = Seq(
      "Authorization" -> appConfig.desAuthorisationToken,
      appConfig.desEnvironmentHeader
    )

    http.GET[Option[String]](appConfig.getCtReferenceUrl(companyNumber), headers = extraHeaders)(
      implicitly[HttpReads[Option[String]]],
      hc,
      implicitly[ExecutionContext]
    )
  }

}