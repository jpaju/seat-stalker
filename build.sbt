Global / onChangedBuildSource := ReloadOnSourceChanges
watchBeforeCommand            := Watch.clearScreen

name    := "seat-stalker"
version := "0.1.0-SNAPSHOT"

// ===========================================================================================
// COMPILER CONFIGURATION
// ===========================================================================================

ThisBuild / scalaVersion := "3.7.2"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-explain",
  "-language:implicitConversions",
  "-Ycheck-all-patmat",
  "-Ycheck-reentrant",
  "-Xkind-projector",
  "-Wsafe-init"
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

// Disable wartremover for tests
ThisBuild / wartremoverExcluded ++= Seq(
  baseDirectory.value,
  (integration / baseDirectory).value
).map(_ / "src" / "test")

// ===========================================================================================
// DEPENDENCY VERSIONS
// ===========================================================================================

val zioVersion         = "2.1.20"
val zioConfigVersion   = "4.0.4"
val zioLoggingVersion  = "2.5.1"
val zioJsonVersion     = "0.7.44"
val zioPreludeversion  = "1.0.0-RC41"
val telegramiumVersion = "9.901.0"
val sttpVersion        = "3.11.0"
val azFunctionVersion  = "3.1.0"

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
      "io.github.apimorphism"         %% "telegramium-core"             % telegramiumVersion,
      "com.softwaremill.sttp.client3" %% "core"                         % sttpVersion,
      "com.softwaremill.sttp.client3" %% "zio"                          % sttpVersion,
      "com.softwaremill.sttp.client3" %% "zio-json"                     % sttpVersion,
      "com.microsoft.azure.functions"  % "azure-functions-java-library" % azFunctionVersion
    ),
    libraryDependencies ++= zioTestDependencies
  )

lazy val integration = (project in file("integration"))
  .dependsOn(root % "compile->compile;test->test")
  .settings(
    publish / skip := true,
    libraryDependencies ++= zioTestDependencies
  )

lazy val zioTestDependencies = Seq(
  "dev.zio" %% "zio-test"          % zioVersion,
  "dev.zio" %% "zio-test-sbt"      % zioVersion,
  "dev.zio" %% "zio-test-magnolia" % zioVersion
).map(_ % Test)

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
