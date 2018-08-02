name := "fs2-experiments"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "0.10.5",
  "co.fs2" %% "fs2-io" % "0.10.5",
  "org.typelevel" %% "cats-effect" % "1.0.0-RC2"
)
scalafmtOnCompile := true