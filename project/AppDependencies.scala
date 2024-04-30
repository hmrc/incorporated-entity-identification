
import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootStrapVersion: String = "8.5.0"
  val mongoVersion: String = "1.9.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"  % mongoVersion
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootStrapVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    "org.playframework" %% "play-test" % current % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    "org.mockito" %% "mockito-scala" % "1.17.31" % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.31" % Test,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.0" % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % mongoVersion % Test
  )

  val it = Seq(
    "com.github.tomakehurst" % "wiremock" % "2.27.2" % Test
  )

}
