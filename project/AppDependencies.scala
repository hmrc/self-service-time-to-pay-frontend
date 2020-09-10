import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {
  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "govuk-template" % "5.56.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.12.0-play-26",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.9.0",
    "com.beachape" %% "enumeratum" % "1.6.1",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.30.0-play-26",

    "uk.gov.hmrc" %% "time-to-pay-taxpayer-cor" % "[0.34.0]",

    "uk.gov.hmrc" %% "domain" %  "5.9.0-play-26",
    "com.typesafe.play" %% "play-json-joda" % "2.6.13"
  )

  val test = Seq(
    "org.pegdown" % "pegdown" % "1.6.0" % "test",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.27.1" % "test",
    "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test",
    "com.softwaremill.macwire" %% "macros" % "2.3.7" % "test",
    "org.seleniumhq.selenium" % "selenium-java" % "3.141.59" % "test",
    "org.seleniumhq.selenium" % "htmlunit-driver" % "2.43.1" % "test"
  )
}
