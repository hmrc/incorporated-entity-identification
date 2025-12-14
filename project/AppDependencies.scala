import play.core.PlayVersion
import sbt.*

object AppDependencies {

  val bootStrapVersion: String = "10.4.0"
  val mongoVersion: String = "2.10.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % mongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"   % bootStrapVersion,
    "org.playframework" %% "play-test"                % PlayVersion.current,
    "org.scalatestplus" %% "mockito-4-11"             % "3.2.18.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"  % mongoVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.2"
  ).map(_ % "test")

  val it: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" % "wiremock" % "3.8.0"
  ).map(_ % "test")

}
