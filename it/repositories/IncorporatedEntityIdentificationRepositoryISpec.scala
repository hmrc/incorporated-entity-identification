
package repositories

import assets.TestConstants._
import play.api.test.Helpers._
import uk.gov.hmrc.incorporatedentityidentification.models.IncorporatedEntityIdentificationModel
import uk.gov.hmrc.incorporatedentityidentification.repositories.IncorporatedEntityIdentificationRepository
import utils.ComponentSpecHelper

import scala.concurrent.ExecutionContext.Implicits.global


class IncorporatedEntityIdentificationRepositoryISpec extends ComponentSpecHelper {
  lazy val repo: IncorporatedEntityIdentificationRepository = app.injector.instanceOf[IncorporatedEntityIdentificationRepository]

  private val testCompanyDetails = IncorporatedEntityIdentificationModel(
    companyNumber = "12345678",
    companyName = "Test Company Ltd",
    ctutr = "1234567890"
  )

  //TODO: Delete later

  "insert" should {
    "successfully insert and retrieve a IncorporatedEntityIdentification model" in {
      val res = for {
        _ <- repo.drop
        _ <- repo.upsert(testJourneyId, testCompanyKey, testCompanyDetails)
        model <- repo.retrieve[IncorporatedEntityIdentificationModel](testJourneyId, testCompanyKey)
      } yield model

      await(res) must contain(testCompanyDetails)
    }
  }

}
