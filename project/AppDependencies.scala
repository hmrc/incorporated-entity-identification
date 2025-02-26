
import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootStrapVersion: String = "8.6.0"
  val mongoVersion: String = "2.5.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % mongoVersion
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"   % bootStrapVersion % Test,
    "org.playframework" %% "play-test"                % current          % Test,
    "org.mockito"       %% "mockito-scala"            % "1.17.37"        % Test,
    "org.mockito"       %% "mockito-scala-scalatest"  % "1.17.37"        % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"  % mongoVersion     % Test,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.18.2"  % Test
  )

  val it = Seq(
    "com.github.tomakehurst" % "wiremock" % "3.0.1" % Test
  )

}
