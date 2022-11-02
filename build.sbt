Global / onChangedBuildSource := ReloadOnSourceChanges
watchBeforeCommand            := Watch.clearScreen

name         := "seat-stalker"
version      := "0.1.0-SNAPSHOT"
scalaVersion := "3.2.0"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-explain",
  "-Ycheck-all-patmat",
  "-Ycheck-reentrant",
  "-Ykind-projector",
  "-Ysafe-init"
) ++ Seq("-source", "future")

val zioVersion        = "2.0.3"
val zioConfigVersion  = "3.0.2"
val zioLoggingVersion = "2.1.3"
val zioJsonVersion    = "0.3.0"
val zioPreludeversion = "1.0.0-RC16"
val sttpVersion       = "3.8.3"
val azFunctionVersion = "2.0.1"

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
)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % zioVersion,
  "dev.zio" %% "zio-test-sbt"      % zioVersion,
  "dev.zio" %% "zio-test-magnolia" % zioVersion
).map(_ % Test)
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

assembly / assemblyOutputPath    := baseDirectory.value / "azure-functions" / "seat-stalker.jar"
assembly / assemblyMergeStrategy := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x                                               =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
