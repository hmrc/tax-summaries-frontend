import sbt._

object FrontendBuild extends Build with MicroService {
  val appName = "tax-summaries-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val frontendbootstrap = "12.4.0"
  private val govukTemplateVersion = "5.30.0-play-25"
  private val urlBuilderVersion = "3.1.0"
  private val httpCachingClientVersion = "8.1.0"
  private val playPartialsVersion = "6.5.0"
  private val domainVersion = "5.3.0"
  private val jSonEncryptionVersion = "4.1.0"

  private val hmrcTestVersion = "3.6.0-play-25"
  private val mockitoAllVersion = "1.10.19"
  private val jSoupVersion = "1.11.3"
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val playLanguageVersion = "3.0.0"
  private val scalaMockVersion = "3.6.0"



  val compile = Seq(
    filters,
    ws,
    "com.typesafe.play" %% "anorm" % "2.5.2",
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendbootstrap,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "json-encryption" % jSonEncryptionVersion,
    "com.mohiva" %% "play-html-compressor" % "0.6.3" // used to pretty print html by stripping out all the whitespaces added by the playframework
   )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
        "org.jsoup" % "jsoup" % jSoupVersion % scope,
        "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
        "org.scalamock" %% "scalamock-scalatest-support" % scalaMockVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()

}
