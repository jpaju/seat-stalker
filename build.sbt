Global / onChangedBuildSource := ReloadOnSourceChanges
watchBeforeCommand            := Watch.clearScreen

name         := "seat-stalker"
version      := "0.1.0-SNAPSHOT"
scalaVersion := "3.2.0"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-explain",
  "-Xfatal-warnings",
  "-Ycheck-all-patmat",
  "-Ycheck-reentrant",
  "-Ykind-projector",
  "-Ysafe-init"
) ++ Seq("-source", "future")

val zioVersion  = "2.0.2"
val sttpVersion = "3.8.2"
libraryDependencies ++= Seq(
  "dev.zio"                       %% "zio"                           % zioVersion,
  "dev.zio"                       %% "zio-json"                      % "0.3.0",
  "dev.zio"                       %% "zio-prelude"                   % "1.0.0-RC16",
  "com.softwaremill.sttp.client3" %% "core"                          % sttpVersion,
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "zio-json"                      % sttpVersion
)
