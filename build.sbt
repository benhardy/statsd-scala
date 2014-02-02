
organization := "net.bhardy"

name := "statsd-scala"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.0"

classpathTypes ~= (_ + "orbit")

javacOptions ++= Seq(
    "-source", "1.6",
    "-target", "1.6"
)

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.0"  % "test",
    "junit" % "junit" % "4.8.1"  % "test",
    "org.mockito" % "mockito-core" % "1.9.5"  % "test"
)

