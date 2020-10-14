package uk.gov.hmrc.incorporatedentityidentification.testonly

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}

class RegisterWithMultipleIdentifiersStubController extends InjectedController{

  val registerWithMultipleIdentifiers: Action[AnyContent] = Action {
    val stubbedSafeId = "X00000123456789"

    Ok(Json.obj(
      "identification" -> Json.arr(
        Json.obj(
          "idType" -> "SAFEID",
          "idValue" -> stubbedSafeId
        )
      )
    ))
  }
}
