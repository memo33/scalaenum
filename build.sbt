name := "scalaenum"

organization := "com.github.memo33"

version := "0.1.5-SNAPSHOT"

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

crossScalaVersions := List("2.11.12", "2.12.17", "2.13.10")  // use `sbt +publishLocal` to publish all versions

scalaVersion := crossScalaVersions.value.last

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-encoding", "UTF-8")

scalacOptions ++= CrossVersion.partialVersion(scalaVersion.value).toSeq.flatMap {
  case ((2, v)) if v <= 11 =>
    Seq(
      "-Yinline-warnings",
      "-optimize",
      "-target:jvm-1.8")
  case ((2, v)) if v >= 12 =>
    Seq(
      "-opt-warnings:at-inline-failed-summary",
      "-opt:l:inline",
      // "-opt-inline-from:**",  // leads to compiler errors with scala-2.13
      "-release:8")
}

Compile / unmanagedSourceDirectories := {
  val sourceDir = (Compile / sourceDirectory).value
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v >= 13 => Seq(sourceDir / "scala-2.13")
    case _ => Seq(sourceDir / "scala")
  }
}

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % "test"
