import sbt._
import play.core.PlayVersion
import play.sbt.PlayImport._

object AppDependencies {

  val compile = Seq(
    filters,
    ws,
    "com.typesafe.play"          %% "anorm"                      % "2.5.3",
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.2",
    "uk.gov.hmrc"                %% "url-builder"                % "3.4.0-play-26",
    "uk.gov.hmrc"                %% "http-caching-client"        % "9.2.0-play-26",
    "uk.gov.hmrc"                %% "bootstrap-frontend-play-26" % "3.4.0",
    "uk.gov.hmrc"                %% "play-language"              % "4.10.0-play-26",
    "uk.gov.hmrc"                %% "govuk-template"             % "5.61.0-play-26",
    "uk.gov.hmrc"                %% "play-ui"                    % "8.21.0-play-26",
    "uk.gov.hmrc"                %% "play-partials"              % "7.1.0-play-26",
    "uk.gov.hmrc"                %% "domain"                     % "5.10.0-play-26",
    "uk.gov.hmrc"                %% "auth-client"                % "3.3.0-play-26",
    "uk.gov.hmrc"                %% "json-encryption"            % "4.8.0-play-26",
    "uk.gov.hmrc"                %% "tax-year"                   % "1.2.0",
    "com.mohiva"                 %% "play-html-compressor"       % "0.7.1", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "com.softwaremill.quicklens" %% "quicklens"                  % "1.6.0",
    "uk.gov.hmrc"                %% "local-template-renderer"    % "2.9.0-play-26",
    "org.typelevel"              %% "cats-core"                  % "2.3.1"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "play-language"                % "4.3.0-play-26",
    "org.jsoup"                % "jsoup"                        % "1.13.1",
    "org.mockito"              % "mockito-all"                  % "1.10.19",
    "uk.gov.hmrc"             %% "hmrctest"                     % "3.9.0-play-26",
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "3.1.3",
    "org.scalamock"           %% "scalamock-scalatest-support"  % "3.6.0",
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current,
    "org.scalacheck"          %% "scalacheck"                   % "1.14.3",
    "com.github.tomakehurst"   % "wiremock-jre8"                % "2.26.1"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}