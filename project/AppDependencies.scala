import sbt._
import play.core.PlayVersion
import play.sbt.PlayImport._

object AppDependencies {

  val playVersion = "play-26"

  val compile = Seq(
    filters,
    ws,
    "com.typesafe.play"          %% "anorm"                      % "2.5.3",
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.2",
    "uk.gov.hmrc"                %% "url-builder"                % s"3.4.0-$playVersion",
    "uk.gov.hmrc"                %% "http-caching-client"        % s"9.2.0-$playVersion",
    "uk.gov.hmrc"                %% "bootstrap-frontend-play-26" % "5.6.0",
    "uk.gov.hmrc"                %% "play-language"              % s"4.10.0-$playVersion",
    "uk.gov.hmrc"                %% "govuk-template"             % s"5.61.0-$playVersion",
    "uk.gov.hmrc"                %% "play-ui"                    % s"9.0.0-$playVersion",
    "uk.gov.hmrc"                %% "play-partials"              % s"7.1.0-$playVersion",
    "uk.gov.hmrc"                %% "domain"                     % s"5.10.0-$playVersion",
    "uk.gov.hmrc"                %% "json-encryption"            % s"4.8.0-$playVersion",
    "uk.gov.hmrc"                %% "tax-year"                   % "1.2.0",
    "com.mohiva"                 %% "play-html-compressor"       % "0.7.1", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "com.softwaremill.quicklens" %% "quicklens"                  % "1.6.0",
    "uk.gov.hmrc"                %% "local-template-renderer"    % s"2.9.0-$playVersion",
    "org.typelevel"              %% "cats-core"                  % "2.3.1"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "play-language"                % s"4.3.0-$playVersion",
    "org.jsoup"                % "jsoup"                        % "1.13.1",
    "org.mockito"              % "mockito-all"                  % "1.10.19",
    "uk.gov.hmrc"             %% "hmrctest"                     % "3.9.0-play-26",
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "3.1.3",
    "org.scalamock"           %% "scalamock-scalatest-support"  % "3.6.0",
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current,
    "org.scalacheck"          %% "scalacheck"                   % "1.14.3",
    "com.github.tomakehurst"   % "wiremock-jre8"                % "2.26.1"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}