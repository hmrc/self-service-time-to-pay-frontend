import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

object FrontendBuild extends Build with MicroService {

  override val appName = "self-service-time-to-pay-frontend"


  override lazy val plugins: Seq[Plugins] = Seq(
    SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
  )

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "1.1.0"
  private val playJsonLoggerVersion = "2.1.1"
  private val frontendBootstrapVersion = "6.7.0"
  private val govukTemplateVersion = "4.0.0"
  private val playUiVersion = "4.18.0"
  private val playPartialsVersion = "4.6.0"
  private val playAuthorisedFrontendVersion = "5.7.0"
  private val playConfigVersion = "2.1.0"
  private val hmrcTestVersion = "1.8.0"
  private val cachingClientVersion = "5.6.0"
  private val playConditionalMappingVersion = "0.2.0"
  private val domainVersion = "3.7.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" %  "8.11.0",
    "uk.gov.hmrc" %% "play-partials" %  "6.1.0",
    "uk.gov.hmrc" %% "domain" %  "4.1.0",
    "uk.gov.hmrc" %% "http-caching-client" %  "7.0.0",
    "uk.gov.hmrc" %% "time" % "3.1.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalMappingVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test,it"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" %  "2.3.0",
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.tomakehurst" % "wiremock" % "2.2.2" % scope,
        "org.mockito" % "mockito-core" % "1.10.19" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()

}


