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

val zioVersion       = "2.0.2"
val zioConfigVersion = "3.0.2"
val sttpVersion      = "3.8.2"

libraryDependencies ++= Seq(
  "dev.zio"                       %% "zio"                           % zioVersion,
  "dev.zio"                       %% "zio-json"                      % "0.3.0",
  "dev.zio"                       %% "zio-prelude"                   % "1.0.0-RC16",
  "dev.zio"                       %% "zio-config"                    % zioConfigVersion,
  "dev.zio"                       %% "zio-config-magnolia"           % zioConfigVersion,
  "com.softwaremill.sttp.client3" %% "core"                          % sttpVersion,
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "zio-json"                      % sttpVersion,
  "com.microsoft.azure.functions"  % "azure-functions-java-library"  % "2.0.1"
)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % "2.0.2",
  "dev.zio" %% "zio-test-sbt"      % "2.0.2",
  "dev.zio" %% "zio-test-magnolia" % "2.0.2"
).map(_ % Test)
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

assembly / assemblyOutputPath    := baseDirectory.value / "azure-functions" / "seat-stalker.jar"
assembly / assemblyMergeStrategy := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x                                               =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
