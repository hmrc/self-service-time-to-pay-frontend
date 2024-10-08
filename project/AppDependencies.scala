
import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val boostrapVersion = "9.4.0"
  val cryptoVersion = "8.0.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"         %% "time-to-pay-taxpayer-cor"              % "0.64.0",
    "uk.gov.hmrc"         %% "play-frontend-hmrc-play-30"            % "10.10.0",
    "uk.gov.hmrc"         %% "bootstrap-frontend-play-30"            % boostrapVersion,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-play-30"                    % "2.2.0",
    "uk.gov.hmrc"         %% "play-conditional-form-mapping-play-30" % "3.2.0",
    "uk.gov.hmrc"         %% "domain-play-30"                        % "10.0.0",
    "uk.gov.hmrc"         %% "crypto-json-play-30"                   % cryptoVersion,
    "com.beachape"        %% "enumeratum-play"                       % "1.8.0",
    "org.julienrf"        %% "play-json-derived-codecs"              % "11.0.0",
    "org.typelevel"       %% "cats-core"                             % "2.12.0"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"                  % "3.2.19",
    "org.wiremock"                % "wiremock-standalone"         % "3.9.1",
    "org.scalatestplus.play"      %% "scalatestplus-play"         % "7.0.1",
    "org.scalatestplus"           %% "scalacheck-1-15"            % "3.2.11.0",
    "com.softwaremill.macwire"    %% "macros"                     % "2.5.9",
    "uk.gov.hmrc"                 %% "bootstrap-test-play-30"     % boostrapVersion
  ).map(_ % Test)
}
