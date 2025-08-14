import play.core.PlayVersion
import sbt.*

object AppDependencies {

  val bootStrapVersion: String = "10.1.0"
  val mongoVersion: String = "2.7.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % mongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"   % bootStrapVersion,
    "org.playframework" %% "play-test"                % PlayVersion.current,
    "org.mockito"       %% "mockito-scala"            % "2.0.0",
    "org.mockito"       %% "mockito-scala-scalatest"  % "2.0.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"  % mongoVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.2"
  ).map(_ % "test")

  val it: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" % "wiremock" % "3.0.1"
  ).map(_ % "test")

}
