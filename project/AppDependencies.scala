import sbt._
import play.core.PlayVersion
import play.sbt.PlayImport._

object AppDependencies {

  val playVersion = "play-28"
  val bootstrapVersion = "7.13.0"

  val compile = Seq(
    filters,
    ws,
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.2",
    "uk.gov.hmrc"                %% "http-caching-client"        % s"10.0.0-$playVersion",
    "uk.gov.hmrc"                %% s"bootstrap-frontend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"                %% "domain"                     % s"8.1.0-$playVersion",
    "uk.gov.hmrc"                %% "tax-year"                   % "3.0.0",
    "com.softwaremill.quicklens" %% "quicklens"                  % "1.6.0",
    "org.typelevel"              %% "cats-core"                  % "2.3.1",
    "uk.gov.hmrc"                %% "play-frontend-hmrc"         % s"6.3.0-play-28",
    "uk.gov.hmrc"                %% "url-builder"                % "3.8.0-play-28"
  )

  val test = Seq(
    "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0",
    "uk.gov.hmrc" %% s"bootstrap-test-play-28" % bootstrapVersion,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.12",
    "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0",
//    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
  "uk.gov.hmrc"             %% "play-language"                % s"6.1.0-$playVersion",
    "org.jsoup"                % "jsoup"                        % "1.13.1",
    "com.typesafe.play"       %% "play-test"                    % PlayVersion.current,
    "com.github.tomakehurst"   % "wiremock-jre8"                % "2.26.1",
    "org.pegdown"             %  "pegdown"                      % "1.6.0",
    "com.vladsch.flexmark"    % "flexmark-all"                  % "0.35.10"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}