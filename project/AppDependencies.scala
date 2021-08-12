
import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-27" % "5.10.0",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "8.0.0-play-27"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-27" % "5.10.0" % Test,
    "org.scalatest" %% "scalatest" % "3.2.9" % "test, it",
    "com.typesafe.play" %% "play-test" % current % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % "test, it",
    "com.github.tomakehurst" % "wiremock-jre8" % "2.29.1" % IntegrationTest,
    "org.mockito" %% "mockito-scala" % "1.16.37" % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.16.37" % Test

  )
}
