import sbt.*
import uk.gov.hmrc.DefaultBuildSettings

val appName = "tax-summaries-frontend"

ThisBuild / majorVersion := 3
ThisBuild / scalaVersion := "3.3.6"
ThisBuild / scalafmtOnCompile := true

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := List(
      "<empty>",
      "Reverse.*",
      ".*.Routes.*",
      "prod.*",
      "views.html.*",
      "uk.gov.hmrc.*",
      "testOnlyDoNotUseInAppConf.*",
      "config.*",
      "models.*",
      "connectors.*",
      "awrs.app.*",
      "views.helpers.*",
      "utils.validation.*",
      "utils.prevalidation.*"
    ).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 85,
    ScoverageKeys.coverageMinimumBranchTotal := 81,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
lazy val microservice      = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9217,
    scoverageSettings,
    libraryDependencies ++= AppDependencies.all
  )
  .settings(
    scalacOptions ++= Seq(
      "-unchecked",
      "-feature",
      "-deprecation",
      "-language:noAutoTupling",
      "-Wvalue-discard",
      "-Werror",
      // TODO DLSN-146: Remove line below and fix deprecation warning
      "-Wconf:msg=.*SafeRedirectUrl is deprecated.*&cat=deprecation:s",
      "-Wconf:src=routes/.*:s",
      "-Wconf:src=views/.*:s",
      "-Wunused:unsafe-warn-patvars",
      "-Wconf:msg=Flag.*repeatedly:s"
    )
  )
  .settings(routesImport ++= Seq("models.admin._"))
  .configs(A11yTest)
  .settings(inConfig(A11yTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings) *)
  .settings(headerSettings(A11yTest) *)
  .settings(automateHeaderSettings(A11yTest))

Test / Keys.fork := true
Test / parallelExecution := true

lazy val it = project
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(
    libraryDependencies ++= AppDependencies.test,
    DefaultBuildSettings.itSettings()
  )

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)
