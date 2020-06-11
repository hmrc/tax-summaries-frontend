import sbt._
import play.core.PlayVersion
import play.sbt.PlayImport._

object AppDependencies {

  val compile = Seq(
    filters,
    ws,
    "com.typesafe.play"          %% "anorm"                % "2.5.2",
    "uk.gov.hmrc"                %% "url-builder"          % "3.4.0-play-25",
    "uk.gov.hmrc"                %% "http-caching-client"  % "9.0.0-play-25",
    "uk.gov.hmrc"                %% "bootstrap-play-25"    % "5.3.0",
    "uk.gov.hmrc"                %% "govuk-template"       % "5.55.0-play-25",
    "uk.gov.hmrc"                %% "play-ui"              % "8.10.0-play-25",
    "uk.gov.hmrc"                %% "play-partials"        % "6.11.0-play-25",
    "uk.gov.hmrc"                %% "domain"               % "5.9.0-play-25",
    "uk.gov.hmrc"                %% "auth-client"          % "3.0.0-play-25",
    "uk.gov.hmrc"                %% "json-encryption"      % "4.5.0-play-25",
    "com.mohiva"                 %% "play-html-compressor" % "0.6.3", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "com.softwaremill.quicklens" %% "quicklens"            % "1.4.12"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "play-language"                % "4.3.0-play-25",
    "org.jsoup"                % "jsoup"                        % "1.11.3",
    "org.mockito"              % "mockito-all"                  % "1.10.19",
    "uk.gov.hmrc"             %% "hmrctest"                     % "3.9.0-play-25",
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "2.0.1",
    "org.scalamock"           %% "scalamock-scalatest-support"  % "3.6.0",
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current,
    "org.scalacheck"          %% "scalacheck"                   % "1.14.1",
    "com.github.tomakehurst"   % "wiremock"               % "2.26.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test

}