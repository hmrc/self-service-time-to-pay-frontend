import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val boostrapVersion = "7.22.0"
  val compile = Seq(
    ws,
    "uk.gov.hmrc"         %% "time-to-pay-taxpayer-cor"           % "0.48.0",
    "uk.gov.hmrc"         %% "play-frontend-hmrc"                 % "7.21.0-play-28",
    "uk.gov.hmrc"         %% "bootstrap-frontend-play-28"         % "7.15.0",
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-play-28"                 % "1.3.0",
    "uk.gov.hmrc"         %% "play-conditional-form-mapping"      % "1.13.0-play-28",
    "uk.gov.hmrc"         %% "time"                               % "3.25.0",   // brakes on newer versions
    "uk.gov.hmrc"         %% "domain"                             % "8.3.0-play-28",
    "com.typesafe.play"   %% "play-json-joda"                     % "2.9.4",
    "com.beachape"        %% "enumeratum-play"                    % "1.7.0",  // upgrade to 1.7.2 is not binary compatable
    "org.julienrf"        %% "play-json-derived-codecs"           % "10.1.0",
    "org.typelevel"       %% "cats-core"                          % "2.9.0",
  )

  val test = Seq(
    "com.github.tomakehurst"      % "wiremock-standalone"         % "2.27.2",
    "org.scalatest"               %% "scalatest"                  % "3.2.15",
    "com.vladsch.flexmark"        %  "flexmark-all"               % "0.62.2",
    "org.scalatestplus.play"      %% "scalatestplus-play"         % "5.1.0",
    "org.scalatestplus"           %% "scalacheck-1-15"            % "3.2.11.0",
    "com.softwaremill.macwire"    %% "macros"                     % "2.5.8",
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"     % boostrapVersion
  ).map(_ % Test)
}
