name := "scalaenum"

organization := "com.github.memo33"

version := "0.1.4"

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

scalaVersion := "2.11.12"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Yinline-warnings",
  "-optimize",
  "-encoding", "UTF-8",
  "-target:jvm-1.6")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % "test"
