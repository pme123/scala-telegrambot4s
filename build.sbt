import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "pme123",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies += "info.mukel" %% "telegrambot4s" % "3.0.14",
    libraryDependencies += scalaTest % Test
  )
