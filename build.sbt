import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import com.typesafe.sbt.uglify.Import._
import com.typesafe.sbt.digest.Import._

val appName = "tax-summaries-frontend"

lazy val plugins: Seq[Plugins] = Seq(
  play.sbt.PlayScala,
  SbtDistributablesPlugin
)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;config.*;models.*;connectors.*;awrs.app.*;view_models.*;views.helpers.*;utils.validation.*;utils.prevalidation.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    PlayKeys.playDefaultPort := 9217,
    scoverageSettings,
    scalaSettings,
    scalaVersion := "2.13.12",
    defaultSettings(),
    majorVersion := 1,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers ++= Seq(Resolver.jcenterRepo),
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(concat, uglify),
    scalafmtOnCompile := true,
    uglify / includeFilter := GlobFilter("ats-*.js")
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings())
  .settings(
    scalacOptions ++= Seq(
      "-feature",
      "-Werror",
      "-Wconf:cat=unused-imports&site=.*views\\.html.*:s",
      "-Wconf:cat=unused-imports&site=<empty>:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s",
      "-Wconf:cat=unused&src=.*JavaScriptReverseRoutes\\.scala:s",
      "-Wconf:cat=other-match-analysis:s"
    )
  )
  .settings(routesImport ++= Seq("models.admin._"))

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("testAll", ";coverage ;test ;it:test ;coverageReport")
addCommandAlias("testAllWithScalafmt", ";scalafmtAll ;testAll")
