
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import sbt.Tests.{Group, SubProcess}


val akkaVersion = "2.5.23"
val akkaHttpVersion = "10.0.15"

dependencyOverrides += "com.typesafe.akka" %% "akka-stream" % akkaVersion
dependencyOverrides += "com.typesafe.akka" %% "akka-protobuf" % akkaVersion
dependencyOverrides += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
dependencyOverrides += "com.typesafe.akka" %% "akka-actor" % akkaVersion
dependencyOverrides += "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }


lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scalaVersion := "2.11.12",
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test )
  .settings(ScalariformSettings())
  .settings(
    unmanagedResourceDirectories in Compile += baseDirectory.value / "public"
  )
  .settings(WartRemoverSettings.wartRemoverError)
  .settings(WartRemoverSettings.wartRemoverWarning)
  .settings(wartremoverExcluded ++=
    routes.in(Compile).value ++
      (baseDirectory.value / "it").get ++
      (baseDirectory.value / "test").get ++
      Seq(sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala"))
  .settings(ScalariformSettings())
  .settings(
    unmanagedSourceDirectories in Test := Seq(baseDirectory.value / "test", baseDirectory.value / "test-common")
  )
  .settings(publishingSettings: _*)
  .settings(resolvers ++= Seq(
    "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/",
    Resolver.bintrayRepo("hmrc", "releases"),
    sbt.Resolver.jcenterRepo
  ))
  .settings(PlayKeys.playDefaultPort := 9063)
  .settings(
    routesImport ++= Seq(
      "langswitch.Language"
    ))
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
      "-Ypartial-unification" //required by cats
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

val appName = "self-service-time-to-pay-frontend"
