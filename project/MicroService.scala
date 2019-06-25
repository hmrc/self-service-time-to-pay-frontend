import play.sbt.PlayImport.PlayKeys
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.sbt.routes.RoutesCompiler.autoImport._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      // Semicolon-separated list of regexs matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;app.Routes.*;prod.*;testOnlyDoNotUseInProd.*;models\\.data\\..*;views.html.*;uk.gov.hmrc.BuildInfo;app.*;prod.*;config.*;uk.gov.hmrc.selfservicetimetopay.auth.*;testonly.testonly.*;uk.gov.hmrc.selfservicetimetopay.models",
      ScoverageKeys.coverageMinimum := 90,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true
    )
  }
  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala,
                       SbtAutoBuildPlugin,
                       SbtGitVersioning,
                       SbtDistributablesPlugin,
                       SbtArtifactory) ++ plugins : _*)
    .settings(playSettings ++ scoverageSettings : _*)
    .settings(scalaSettings: _*)
    .settings(scalacOptions += "-Ypartial-unification")
    .settings(PlayKeys.playDefaultPort := 9063)
    .settings(majorVersion := 0)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      routesGenerator := StaticRoutesGenerator,
      scalaVersion := "2.11.11",
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesImport ++= Seq(
//        "uk.gov.hmrc.selfservicetimetopay.auth.Token",
//        "uk.gov.hmrc.selfservicetimetopay.auth.Token._"
      )
    )
    .settings(scalariformSettings: _*)
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := TestPhases.oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false)
    .settings(resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      sbt.Resolver.jcenterRepo
    ))

  lazy val scalariformSettings =
  // description of options found here -> https://github.com/scala-ide/scalariform
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignArguments, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AllowParamGroupsOnNewlines, true)
      .setPreference(CompactControlReadability, false)
      .setPreference(CompactStringConcatenation, false)
      .setPreference(DanglingCloseParenthesis, Preserve)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(DoubleIndentMethodDeclaration, true)
      .setPreference(FirstArgumentOnNewline, Preserve)
      .setPreference(FirstParameterOnNewline, Preserve)
      .setPreference(FormatXml, true)
      .setPreference(IndentLocalDefs, true)
      .setPreference(IndentPackageBlocks, true)
      .setPreference(IndentSpaces, 2)
      .setPreference(IndentWithTabs, false)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
      .setPreference(NewlineAtEndOfFile, true)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, false)
      .setPreference(PreserveSpaceBeforeArguments, true)
      .setPreference(RewriteArrowSymbols, false)
      .setPreference(SpaceBeforeColon, false)
      .setPreference(SpaceBeforeContextColon, false)
      .setPreference(SpaceInsideBrackets, false)
      .setPreference(SpaceInsideParentheses, false)
      .setPreference(SpacesAroundMultiImports, false)
      .setPreference(SpacesWithinPatternBinders, true)
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Tests.Group] =
    tests map {
      test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}

