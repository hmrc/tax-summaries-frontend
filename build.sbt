import sbt.*
import uk.gov.hmrc.DefaultBuildSettings

val appName = "tax-summaries-frontend"

ThisBuild / majorVersion := 3
ThisBuild / scalaVersion := "3.3.4"
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
lazy val microservice = Project(appName, file("."))
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
      "-Xfatal-warnings",
      "-language:noAutoTupling",
      "-Wunused:imports",
      "-Wvalue-discard",
      "-Werror",
      "-Wconf:msg=unused import&src=.*views\\.html.*:s",
      "-Wconf:msg=unused import&src=<empty>:s",
      "-Wconf:msg=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:msg=unused&src=.*Routes\\.scala:s",
      "-Wconf:msg=unused&src=.*ReverseRoutes\\.scala:s",
      "-Wconf:msg=unused&src=.*JavaScriptReverseRoutes\\.scala:s",
      "-Wconf:msg=other-match-analysis:s",
      "-Wconf:msg=trait HttpClient in package http is deprecated.*:s",
      "-Wconf:msg=a type was inferred to be `Object`; this may indicate a programming error\\.&src=.*Spec\\.scala:s",
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
