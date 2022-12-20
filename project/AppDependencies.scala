import sbt._
import play.core.PlayVersion
import play.sbt.PlayImport._

object AppDependencies {

  val playVersion = "play-28"

  val compile = Seq(
    filters,
    ws,
//    "com.typesafe.play"          %% "anorm"                      % "2.7.0",
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.5",
    "uk.gov.hmrc"                %% "http-caching-client"        % s"10.0.0-$playVersion",
    "uk.gov.hmrc"                %% s"bootstrap-frontend-$playVersion" % "7.12.0",
    "uk.gov.hmrc"                %% "play-partials"              % s"8.3.0-$playVersion",
    "uk.gov.hmrc"                %% "domain"                     % s"8.1.0-$playVersion",
    "uk.gov.hmrc"                %% "tax-year"                   % "3.0.0",
    "com.softwaremill.quicklens" %% "quicklens"                  % "1.6.0",
    "org.typelevel"              %% "cats-core"                  % "2.3.1",
    "uk.gov.hmrc"                %% "play-frontend-hmrc"         % s"3.17.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "play-language"                % s"5.4.0-$playVersion",
    "org.jsoup"                % "jsoup"                        % "1.15.3",
    "uk.gov.hmrc"             %% s"bootstrap-test-play-28"      % "7.12.0",
    "org.mockito"             %% "mockito-scala-scalatest"      % "1.17.12",
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current,
    "org.scalatestplus"       %% "scalacheck-1-16"               % "3.2.14.0",
//    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
  "com.github.tomakehurst"   % "wiremock-jre8"                % "2.26.1",
    "org.pegdown"             %  "pegdown"                      % "1.6.0",
    "com.vladsch.flexmark"    % "flexmark-all"                  % "0.62.2"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}