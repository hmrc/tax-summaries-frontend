import sbt._
import play.core.PlayVersion
import play.sbt.PlayImport._

object AppDependencies {

  val compile = Seq(
    filters,
    ws,
    "com.typesafe.play" %% "anorm" % "2.5.2",
    "uk.gov.hmrc" %% "url-builder" % "3.1.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.30.0-play-25",
    "uk.gov.hmrc" %% "http-caching-client" % "8.1.0",
    "uk.gov.hmrc" %% "frontend-bootstrap" % "12.4.0",
    "uk.gov.hmrc" %% "play-partials" % "6.5.0",
    "uk.gov.hmrc" %% "domain" % "5.3.0",
    "uk.gov.hmrc" %% "json-encryption" % "4.1.0",
    "com.mohiva" %% "play-html-compressor" % "0.6.3" // used to pretty print html by stripping out all the whitespaces added by the playframework
  )

  val test = Seq(
    "uk.gov.hmrc" %% "play-language" % "3.0.0",
    "org.jsoup" % "jsoup" % "1.11.3",
    "org.mockito" % "mockito-all" % "1.10.19",
    "uk.gov.hmrc" %% "hmrctest" % "3.6.0-play-25",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0",
    "com.typesafe.play" %% "play-test" % PlayVersion.current
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test

}