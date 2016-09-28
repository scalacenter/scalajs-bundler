sbtPlugin := true

name := "sbt-scalajs-bundler"

organization := "ch.epfl.scala"

description := "Module bundler for Scala.js projects"

version := "0.1-SNAPSHOT"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.13-SNAPSHOT")

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts += "-Dplugin.version=" + version.value

scriptedBufferLog := false