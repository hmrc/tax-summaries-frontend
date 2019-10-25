import play.routes.compiler.StaticRoutesGenerator
import sbt._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile
import play.sbt.routes.RoutesKeys.routesGenerator

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.SbtAutoBuildPlugin
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning
  import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
  import uk.gov.hmrc.SbtArtifactory

  

  val appName: String

  val appDependencies : Seq[ModuleID]
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory, SbtDistributablesPlugin)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      // Semicolon-separated list of regexs matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;config.*;models.*;connectors.*;awrs.app.*;view_models.*;views.helpers.*;",
      ScoverageKeys.coverageMinimum := 80,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true,
      parallelExecution in Test := false
    )
  }

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(plugins : _*)
    .settings(
      scoverageSettings,
      scalaSettings,
      publishingSettings,
      defaultSettings(),
      majorVersion := 1,
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      scalafmtOnCompile := true,
      evictionWarningOptions in update :=
        EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
        routesGenerator := StaticRoutesGenerator
    )
    .settings(resolvers ++= Seq(Resolver.bintrayRepo("hmrc", "releases"),Resolver.jcenterRepo))
}

