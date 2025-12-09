import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val playVersion          = "play-30"
  private val scaWrapperVersion    = "4.5.0"
  private val featureToggleVersion = "2.4.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    ws,
    "uk.gov.hmrc"   %% "tax-year"                                   % "6.0.0",
    "org.typelevel" %% "cats-core"                                  % "2.13.0",
    "uk.gov.hmrc"   %% s"mongo-feature-toggles-client-$playVersion" % featureToggleVersion,
    "uk.gov.hmrc"   %% s"sca-wrapper-$playVersion"                  % scaWrapperVersion
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"          % "jsoup"                                           % "1.21.2",
    "uk.gov.hmrc"       %% s"mongo-feature-toggles-client-test-$playVersion" % featureToggleVersion,
    "uk.gov.hmrc"       %% s"sca-wrapper-test-$playVersion"                  % scaWrapperVersion,
    "org.scalatestplus" %% "scalacheck-1-18"                                 % "3.2.19.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
