import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {
  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.12.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "5.5.0-play-28",

    "uk.gov.hmrc" %%  "play-conditional-form-mapping" % "1.9.0-play-28",

    "com.beachape" %% "enumeratum" % "1.6.1",
    "uk.gov.hmrc" %% "time" % "3.25.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "0.73.0",

    "uk.gov.hmrc" %% "time-to-pay-taxpayer-cor" % "0.40.0",

    "uk.gov.hmrc" %% "domain" %  "6.0.0-play-28",
    "com.typesafe.play" %% "play-json-joda" % "2.7.4",

    "com.beachape" %% "enumeratum-play" % "1.7.0",
    "org.julienrf" %% "play-json-derived-codecs" % "10.1.0",

    "org.typelevel" %% "cats-core" % "2.8.0",

  )

  val test = Seq(
    "com.github.tomakehurst" % "wiremock-standalone" % "2.27.1" % "test",
    "org.scalatest" %% "scalatest" % "3.1.0" % "test",
    "com.vladsch.flexmark" %  "flexmark-all" % "0.35.10" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test",
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
    "com.softwaremill.macwire" %% "macros" % "2.3.7" % "test",
    "org.seleniumhq.selenium" % "selenium-java" % "3.141.59" % "test",
    "org.seleniumhq.selenium" % "htmlunit-driver" % "2.45.0" % "test"
  )
}
