import sbt.Keys.libraryDependencies
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
    "uk.gov.hmrc" %% "govuk-template" % "5.26.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "7.40.0-play-26",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.41.0",
    "com.beachape" %% "enumeratum" % "1.5.13",

    "uk.gov.hmrc" %% "play-partials" %  "6.9.0-play-26",
    "uk.gov.hmrc" %% "domain" %  "5.2.0",
    "uk.gov.hmrc" %% "time" % "3.1.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "com.typesafe.play" %% "play-json-joda" % "2.6.13",

    "uk.gov.hmrc" %% "http-caching-client" % "8.4.0-play-26",

    "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided" ,
    "com.softwaremill.macwire" %% "macrosakka" % "2.3.0" % "provided" ,
    "com.softwaremill.macwire" %% "util" % "2.3.0" % "test",
    "com.softwaremill.macwire" %% "proxy" % "2.3.0" % "test",

    "org.seleniumhq.selenium" % "selenium-java" % "3.141.59" % "test",
    "org.seleniumhq.selenium" % "htmlunit-driver" % "2.35.1" % "test",
    "net.sourceforge.htmlunit" % "htmlunit" % "2.35" % "test"


  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.tomakehurst" % "wiremock-standalone" % "2.12.0" % "test",

        "org.mockito" % "mockito-core" % "1.10.19" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % scope,
        "org.typelevel" %% "cats-core" % "1.4.0"
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()

}


