ThisBuild / name := "scala-experiments"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.1"

lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % "2.0.0")
lazy val fs2IO = Seq(
  "co.fs2" %% "fs2-core" % "2.2.1",
  "co.fs2" %% "fs2-io" % "2.2.1",
)
// lazy val shapeless = Seq("com.chuusai" %% "shapeless" % "2.3.3")
lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % "3.1.0" % "test")

lazy val common = (project in file("common"))
  .settings(
    libraryDependencies ++= catsEffect ++ scalaTest
  )

lazy val fetchfile = (project in file("fetch-file"))
  .settings(
    libraryDependencies ++= catsEffect ++ fs2IO ++ /* shapeless ++ */ scalaTest
  )
  .dependsOn(common)

lazy val root = (project in file("."))
  .aggregate(fetchfile, common)
