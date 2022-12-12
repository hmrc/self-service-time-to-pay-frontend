
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.defaultSettings

val appName = "self-service-time-to-pay-frontend"
val scalaV = "2.12.12"

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }


lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(defaultSettings(): _*)
  .settings(
    scalaVersion := scalaV,
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test )
  .settings(ScalariformSettings())
  .settings(WartRemoverSettings.wartRemoverError)
  .settings(WartRemoverSettings.wartRemoverWarning)
  .settings(wartremoverExcluded ++=
    routes.in(Compile).value ++
      (baseDirectory.value / "test").get ++
      Seq(sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala"))
  .settings(ScalariformSettings())
  .settings(
    unmanagedSourceDirectories in Test := Seq(baseDirectory.value / "test", baseDirectory.value / "test-common")
  )
  .settings(publishingSettings: _*)
  .settings(resolvers ++= Seq(
    sbt.Resolver.jcenterRepo
  ))
  .settings(PlayKeys.playDefaultPort := 9063)
  .settings(
    routesImport ++= Seq(
      "langswitch.Language"
    ),
    sources in (Compile,doc) := Seq.empty,
    publishArtifact in packageDoc := false
  )
  .settings(
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-value-discard",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-Ypartial-unification", //required by cats
      "-Ywarn-unused:-imports,-patvars,-privates,-locals,-explicits,-implicits,_"
    )
  )
  .settings(
    commands += Command.command("runTestOnly") { state =>
      state.globalLogging.full.info("running play using 'testOnlyDoNotUseInProd' routes...")
      s"""set javaOptions += "-Dplay.http.router=testOnlyDoNotUseInProd.Routes"""" ::
        "run" ::
        s"""set javaOptions -= "-Dplay.http.router=testOnlyDoNotUseInProd.Routes"""" ::
        state
    }
  )