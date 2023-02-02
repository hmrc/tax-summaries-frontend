import sbt._
import play.core.PlayVersion
import play.sbt.PlayImport._

object AppDependencies {

  val playVersion = "play-28"

  val compile = Seq(
    filters,
    ws,
    "com.typesafe.play"          %% "anorm"                      % "2.5.3",
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.2",
    "uk.gov.hmrc"                %% "http-caching-client"        % s"9.5.0-$playVersion",
    "uk.gov.hmrc"                %% s"bootstrap-frontend-$playVersion" % "5.7.0",
    "uk.gov.hmrc"                %% "domain"                     % s"8.1.0-$playVersion",
    "uk.gov.hmrc"                %% "tax-year"                   % "3.0.0",
    "com.mohiva"                 %% "play-html-compressor"       % "0.7.1", // used to pretty print html by stripping out all the whitespaces added by the playframework
    "com.softwaremill.quicklens" %% "quicklens"                  % "1.6.0",
    "org.typelevel"              %% "cats-core"                  % "2.3.1",
    "uk.gov.hmrc"                %% "play-frontend-hmrc"         % s"6.3.0-play-28",
    "uk.gov.hmrc"                %% "url-builder"                % "3.8.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "play-language"                % s"5.1.0-$playVersion",
    "org.jsoup"                % "jsoup"                        % "1.13.1",
    "org.mockito"              % "mockito-all"                  % "1.10.19",
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "5.1.0",
    "org.scalatestplus"       %% "scalatestplus-scalacheck"     % "3.1.0.0-RC2",
    "org.scalatestplus"       %% "scalatestplus-mockito"        % "1.0.0-M2",
    "org.scalamock"           %% "scalamock-scalatest-support"  % "3.6.0",
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current,
    "org.scalacheck"          %% "scalacheck"                   % "1.14.3",
    "com.github.tomakehurst"   % "wiremock-jre8"                % "2.26.1",
    "org.pegdown"             %  "pegdown"                      % "1.6.0",
    "com.vladsch.flexmark"    % "flexmark-all"                  % "0.35.10"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}