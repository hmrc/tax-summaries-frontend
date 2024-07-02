import play.sbt.PlayImport.*
import sbt.*


object AppDependencies {

  val playVersion = "play-30"
  val bootstrapVersion = "8.4.0"
  lazy val hmrcMongoVersion: String = "1.6.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    ws,
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.5",
    "uk.gov.hmrc"                %% "tax-year"                   % "4.0.0",
    "org.typelevel"              %% "cats-core"                  % "2.12.0",
    "uk.gov.hmrc"               %% s"mongo-feature-toggles-client-$playVersion"     % "1.1.0",
    "uk.gov.hmrc" %% s"sca-wrapper-$playVersion" % "1.3.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"                   % "jsoup"                   % "1.17.2",
    "uk.gov.hmrc"                %% s"bootstrap-test-$playVersion" % bootstrapVersion,
    "org.mockito"       %% "mockito-scala-scalatest" % "1.17.31",
    "org.scalatestplus"          %% "scalacheck-1-17"         % "3.2.18.0",
    "com.softwaremill.quicklens" %% "quicklens"               % "1.9.7",
    "uk.gov.hmrc.mongo"   %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion
  ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
