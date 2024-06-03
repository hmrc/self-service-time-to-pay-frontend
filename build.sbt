
import uk.gov.hmrc.DefaultBuildSettings.defaultSettings

val appName = "self-service-time-to-pay-frontend"
val appScalaVersion = "2.13.12"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(defaultSettings(): _*)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings: _*)
  .settings(
    scalaVersion := appScalaVersion,
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    Test / parallelExecution := false
  )
  .settings(ScoverageSettings())
  .settings(ScalariformSettings())
  .settings(WartRemoverSettings.wartRemoverError)
  .settings(WartRemoverSettings.wartRemoverWarning)
  .settings(wartremoverExcluded ++=
    (Compile / routes).value ++
      (baseDirectory.value / "test").get ++
        target.value.get ++
          Seq(sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala"))
  .settings(ScalariformSettings())
  .settings(
    Test / unmanagedSourceDirectories := Seq(baseDirectory.value / "test", baseDirectory.value / "test-common")
  )
  .settings(resolvers ++= Seq(
    sbt.Resolver.jcenterRepo
  ))
  .settings(PlayKeys.playDefaultPort := 9063)
  .settings(
    Compile / doc / sources  := Seq.empty,
    packageDoc / publishArtifact  := false
  )
  .settings(
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Xlint:adapted-args",
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:params",
      "-Ywarn-unused:patvars",
      "-Ywarn-unused:privates",
      "-Ywarn-value-discard",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      // required in place of silencer plugin
      "-Wconf:cat=unused-imports&src=html/.*:s",
      "-Wconf:src=routes/.*:s"
    )
  )
  .settings(
    commands += Command.command("runTestOnly") { state =>
      state.globalLogging.full.info("running play using 'testOnlyDoNotUseInAppConf' routes...")
      s"""set javaOptions += "-Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"""" ::
        "run" ::
        s"""set javaOptions -= "-Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"""" ::
        state
    }
  )