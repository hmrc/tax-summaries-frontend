import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._


object AppDependencies {

  val playVersion = "play-28"
  val bootstrapVersion = "7.15.0"
  lazy val hmrcMongoVersion: String = "1.1.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    ws,
    "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.5",
    "uk.gov.hmrc"                %% "http-caching-client"        % s"10.0.0-$playVersion",
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"                %% "domain"                     % s"8.3.0-$playVersion",
    "uk.gov.hmrc"                %% "tax-year"                   % "3.2.0",
    "org.typelevel"              %% "cats-core"                  % "2.9.0",
    ehcache,
    "uk.gov.hmrc"               %% "mongo-feature-toggles-client"     % "0.2.0",
    "uk.gov.hmrc"               %% "sca-wrapper"                      % "1.0.41",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % s"7.7.0-play-28",
    "uk.gov.hmrc" %% "play-partials" % s"8.4.0-$playVersion",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion" % hmrcMongoVersion,
    "uk.gov.hmrc" %% s"internal-auth-client-$playVersion" % "1.4.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% "play-language"           % s"6.1.0-$playVersion",
    "org.jsoup"                   % "jsoup"                   % "1.16.1",
    "uk.gov.hmrc"                %% s"bootstrap-test-play-28" % bootstrapVersion,
    "org.mockito"                %% "mockito-scala-scalatest" % "1.17.14",
    "com.typesafe.play"          %% "play-test"               % PlayVersion.current,
    "org.scalatestplus"          %% "scalacheck-1-17"         % "3.2.15.0",
    "com.github.tomakehurst"      % "wiremock-jre8"           % "2.35.0",
    "org.pegdown"                 % "pegdown"                 % "1.6.0",
    "com.softwaremill.quicklens" %% "quicklens"               % "1.6.0",
    "com.vladsch.flexmark"        % "flexmark-all"            % "0.62.2",
    "uk.gov.hmrc.mongo"          %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion
  ).map(_ % "test,it")

  val jacksonVersion = "2.13.2"
  val jacksonDatabindVersion = "2.13.2.2"

  val jacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-core",
    "com.fasterxml.jackson.core" % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion)

  val jacksonDatabindOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion
  )

  val akkaSerializationJacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module" % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module" %% "jackson-module-scala",
  ).map(_ % jacksonVersion)

  val all: Seq[ModuleID] = compile ++ jacksonDatabindOverrides ++ jacksonOverrides ++ akkaSerializationJacksonOverrides ++ test
}
