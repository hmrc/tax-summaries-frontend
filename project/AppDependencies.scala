import play.sbt.PlayImport.*
import sbt.*


object AppDependencies {

  private val playVersion = "play-30"
  private val scaWrapperVersion = "1.12.0"
  private val featureToggleVersion = "1.6.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    ws,
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.5",
    "uk.gov.hmrc"                %% "tax-year"                   % "4.0.0",
    "org.typelevel"              %% "cats-core"                  % "2.12.0",
    "uk.gov.hmrc"                %% s"mongo-feature-toggles-client-$playVersion" % featureToggleVersion,
    "uk.gov.hmrc"                %% s"sca-wrapper-$playVersion"                  % scaWrapperVersion
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"                   % "jsoup"                   % "1.18.1",
    "com.softwaremill.quicklens" %% "quicklens"               % "1.9.7",
    "uk.gov.hmrc"                %% s"mongo-feature-toggles-client-test-$playVersion" % featureToggleVersion,
    "uk.gov.hmrc"                %% s"sca-wrapper-test-$playVersion"                  % scaWrapperVersion,
    "org.scalatestplus"          %% "scalacheck-1-17"         % "3.2.18.0"
  ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
