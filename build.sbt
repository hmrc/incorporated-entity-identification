import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.4"

val appName = "incorporated-entity-identification"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    scalafmtOnCompile := true
  )
  .settings(CodeCoverageSettings.settings *)

scalacOptions ++= Seq(
  "-Werror",
  "-Wconf:src=routes/.*:s",
  "-Wconf:msg=Flag.*repeatedly:s"
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
