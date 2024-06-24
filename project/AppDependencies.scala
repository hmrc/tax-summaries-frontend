import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._


object AppDependencies {

  val playVersion = "play-30"
  val bootstrapVersion = "8.4.0"
  lazy val hmrcMongoVersion: String = "1.6.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    ws,
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.5",
    "uk.gov.hmrc"                %% s"http-caching-client-$playVersion"        % s"11.1.0",
    "uk.gov.hmrc"                %% "tax-year"                   % "4.0.0",
    "org.typelevel"              %% "cats-core"                  % "2.10.0",
    "uk.gov.hmrc"               %% s"mongo-feature-toggles-client-$playVersion"     % "1.1.0",
    "uk.gov.hmrc" %% s"sca-wrapper-$playVersion" % "1.3.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"                   % "jsoup"                   % "1.16.1",
    "uk.gov.hmrc"                %% s"bootstrap-test-$playVersion" % bootstrapVersion,
    "org.mockito"       %% "mockito-scala-scalatest" % "1.17.30",
    "org.scalatestplus"          %% "scalacheck-1-17"         % "3.2.17.0",
    "com.softwaremill.quicklens" %% "quicklens"               % "1.9.6"
  ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
