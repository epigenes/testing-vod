import com.typesafe.sbt.packager.docker.DockerChmodType

name := """test-s3"""
organization := "epigenes"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies += guice
libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.11.555"
libraryDependencies += "org.typelevel" %% "cats-core" % "1.6.0"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test

// Adds additional packages into Twirl
// TwirlKeys.templateImports += "epigenes.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "epigenes.binders._"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

dockerExposedPorts ++= Seq(9000)

// Required to run in a docker container
javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

dockerChmodType := DockerChmodType.UserGroupWriteExecute