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

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" %  "12.1.0",
    "uk.gov.hmrc" %% "play-partials" %  "6.3.0",
    "uk.gov.hmrc" %% "domain" %  "5.2.0",
    "uk.gov.hmrc" %% "http-caching-client" %  "8.0.0",
    "uk.gov.hmrc" %% "time" % "3.1.0",
    "uk.gov.hmrc" %% "play-language" % "3.4.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc" %% "hmrctest" %  "3.0.0",
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.tomakehurst" % "wiremock" % "2.2.2" % scope,
        "org.mockito" % "mockito-core" % "1.10.19" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
        "org.typelevel" %% "cats-core" % "1.4.0"
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()

}


