Global / onChangedBuildSource := ReloadOnSourceChanges
watchBeforeCommand            := Watch.clearScreen

name         := "seat-stalker"
version      := "0.1.0-SNAPSHOT"
scalaVersion := "3.3.6"

// ===========================================================================================
// COMPILER CONFIGURATION
// ===========================================================================================

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-explain",
  "-language:implicitConversions",
  "-Ycheck-all-patmat",
  "-Ycheck-reentrant",
  "-Ykind-projector",
  "-Ysafe-init"
) ++ Seq("-source", "future")

// ===========================================================================================
// LINTER CONFIGURATION
// ===========================================================================================

// Report all warnings as errors only on compile, not in e.g REPL or tests
ThisBuild / scalacOptions ++= Seq(
  "-Wvalue-discard",           // Warn when function evaluates to value that is discarded (because return type is Unit)
  "-Wnonunit-statement",       // Warn when non-Unit expression is in statement position
  "-Wunused:all",              // Warn on unused imports, params, privates, locals, etc
  "-Wconf:msg=unused:warning", // Set unused warnings to warning level
  "-Wconf:any:error"           // Promote any other warnings to errors (i.e. treat warnings as errors, like -Xfatal-warnings)
)

// List of all warts: https://www.wartremover.org/doc/warts.html
ThisBuild / wartremoverWarnings ++= Warts.allBut(
  Wart.Any,
  Wart.Equals,
  Wart.ImplicitConversion,
  Wart.ImplicitParameter,
  Wart.Nothing,
  Wart.Overloading,
  Wart.Recursion,
  Wart.FinalCaseClass,
  Wart.ToString
)

// Disable for tests
ThisBuild / wartremoverExcluded ++= Seq("test", "it").map(baseDirectory.value / "src" / _)

// ===========================================================================================
// DEPENDENCY VERSIONS
// ===========================================================================================

val zioVersion        = "2.1.19"
val zioConfigVersion  = "3.0.7"
val zioLoggingVersion = "2.5.1"
val zioJsonVersion    = "0.7.44"
val zioPreludeversion = "1.0.0-RC41"
val sttpVersion       = "3.11.0"
val azFunctionVersion = "2.2.0"

// ===========================================================================================
// PROJECT CONFIGURATION
// ===========================================================================================

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"                       %% "zio"                          % zioVersion,
      "dev.zio"                       %% "zio-json"                     % zioJsonVersion,
      "dev.zio"                       %% "zio-prelude"                  % zioPreludeversion,
      "dev.zio"                       %% "zio-logging"                  % zioLoggingVersion,
      "dev.zio"                       %% "zio-config"                   % zioConfigVersion,
      "dev.zio"                       %% "zio-config-magnolia"          % zioConfigVersion,
      "com.softwaremill.sttp.client3" %% "core"                         % sttpVersion,
      "com.softwaremill.sttp.client3" %% "zio"                          % sttpVersion,
      "com.softwaremill.sttp.client3" %% "zio-json"                     % sttpVersion,
      "com.microsoft.azure.functions"  % "azure-functions-java-library" % azFunctionVersion
    ),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test"          % zioVersion,
      "dev.zio" %% "zio-test-sbt"      % zioVersion,
      "dev.zio" %% "zio-test-magnolia" % zioVersion
    ).map(_ % "test,it"),
    Defaults.itSettings
  )
  .configs(DeepIntegrationTest)

lazy val DeepIntegrationTest =
  IntegrationTest.extend(Test) // Required for bloop https://github.com/scalacenter/bloop/issues/1162

// ===========================================================================================
// PACKAGING/DEPLOYMENT CONFIGURATION
// ===========================================================================================

assembly / assemblyOutputPath    := baseDirectory.value / "azure-functions" / "seat-stalker.jar"
assembly / assemblyMergeStrategy := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x                                               =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
