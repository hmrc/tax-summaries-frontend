import sbt._

object FrontendBuild extends Build with MicroService {
  val appName = "tax-summaries-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val frontendbootstrap = "8.17.0"
  private val govukTemplateVersion = "5.3.0"
  private val urlBuilderVersion = "2.0.0"
  private val httpCachingClientVersion = "7.1.0"
  private val playPartialsVersion = "6.1.0"
  private val domainVersion = "5.1.0"
  private val jSonEncryptionVersion = "3.2.0"

  private val pegDownVersion = "1.6.0"
  private val hmrcTestVersion = "2.3.0"
  private val mockitoAllVersion = "1.10.19"
  private val jSoupVersion = "1.8.3"
  private val scalaTestPlusPlayVersion = "1.5.1"
  private val scalatestVersion = "2.2.6"
  private val playLanguageVersion = "3.0.0"

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
        "org.pegdown" % "pegdown" % pegDownVersion % scope,
        "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
        "org.jsoup" % "jsoup" % jSoupVersion % scope,
        "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()

}
