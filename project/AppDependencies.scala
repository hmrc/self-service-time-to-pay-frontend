
import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val boostrapVersion = "7.22.0"
  val cryptoVersion = "7.3.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"         %% "time-to-pay-taxpayer-cor"           % "0.57.0",
    "uk.gov.hmrc"         %% "play-frontend-hmrc"                 % "7.25.0-play-28",
    "uk.gov.hmrc"         %% "bootstrap-frontend-play-28"         % boostrapVersion,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-play-28"                 % "1.3.0",
    "uk.gov.hmrc"         %% "play-conditional-form-mapping"      % "1.13.0-play-28",
    "uk.gov.hmrc"         %% "domain"                             % "8.3.0-play-28",
    "uk.gov.hmrc"         %% "crypto-json-play-28"                % cryptoVersion,
    "com.typesafe.play"   %% "play-json-joda"                     % "2.9.4",
    "com.beachape"        %% "enumeratum-play"                    % "1.7.0",  // upgrade to 1.7.1 is incompatible
    "org.julienrf"        %% "play-json-derived-codecs"           % "10.1.0",
    "org.typelevel"       %% "cats-core"                          % "2.10.0"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"                  % "3.2.17",
    "org.wiremock"                % "wiremock-standalone"         % "3.3.0",
    "com.vladsch.flexmark"        %  "flexmark-all"               % "0.64.8",
    "org.scalatestplus.play"      %% "scalatestplus-play"         % "5.1.0",
    "org.scalatestplus"           %% "scalacheck-1-15"            % "3.2.11.0",
    "com.softwaremill.macwire"    %% "macros"                     % "2.5.9",
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"     % boostrapVersion
  ).map(_ % Test)
}
