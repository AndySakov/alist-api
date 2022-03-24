import Dependencies._

ThisBuild / organization := "com.northstarr"
ThisBuild / scalaVersion := "2.13.8"

lazy val `alist-api` =
  project
    .in(file("."))
    .settings(name := "alist-api")
    .settings(commonSettings)
    .settings(dependencies)

lazy val commonSettings = commonScalacOptions ++ Seq(
  update / evictionWarningOptions := EvictionWarningOptions.empty
)

lazy val commonScalacOptions = Seq(
  Compile / console / scalacOptions --= Seq(
    "-Wunused:_",
    "-Xfatal-warnings",
  ),
  Test / console / scalacOptions :=
    (Compile / console / scalacOptions).value,
)

val ZHTTPVersion = "1.0.0.0-RC25"
val loggingVersion = "0.5.1"

lazy val dependencies = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "1.0.13",
    "dev.zio" %% "zio-interop-cats"  % "2.1.4.0",
    "io.d11" %% "zhttp"      % ZHTTPVersion,
    "io.d11" %% "zhttp-test" % ZHTTPVersion % Test,
    "io.scalac" %% "zio-slick-interop" % "0.4",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
    "com.h2database" % "h2" % "1.4.200",
    "dev.zio" %% "zio-logging"       % loggingVersion,
    "dev.zio" %% "zio-logging-slf4j" % loggingVersion,
    "dev.zio" %% "zio-json" % "0.1.5"
  ),
  libraryDependencies ++= Seq(
    org.scalatest.scalatest,
    org.scalatestplus.`scalacheck-1-15`,
  ).map(_ % Test),
)
