import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.Import.WebKeys._
import net.ground5hark.sbt.concat.Import._
import com.typesafe.sbt.uglify.Import._
import com.typesafe.sbt.digest.Import._

val appName = "tax-summaries-frontend"

lazy val plugins: Seq[Plugins] = Seq(
  play.sbt.PlayScala,
  SbtAutoBuildPlugin,
  SbtGitVersioning,
  SbtDistributablesPlugin
)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;config.*;models.*;connectors.*;awrs.app.*;view_models.*;views.helpers.*;utils.validation.*;utils.prevalidation.*;",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    PlayKeys.playDefaultPort := 9217,
    scoverageSettings,
    scalaSettings,
    scalaVersion := "2.12.12",
    publishingSettings,
    defaultSettings(),
    majorVersion := 1,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    evictionWarningOptions in update :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo,
      "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"),
    Concat.groups := Seq(
      "javascripts/ats-app.js" -> group(
        Seq(
          "javascripts/jquery.min.js",
          "javascripts/jquery-ui.min.js",
          "javascripts/magna-charta.js",
          "javascripts/details.polyfill.js",
          "javascripts/taxs.js"))
    ),
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    pipelineStages in Assets := Seq(concat, uglify),
    scalafmtOnCompile := true,
    includeFilter in uglify := GlobFilter("ats-*.js")
  )
