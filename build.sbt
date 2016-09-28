sbtPlugin := true

name := "sbt-scalajs-bundler"

organization := "ch.epfl.scala"

description := "Module bundler for Scala.js projects"

version := "0.1-SNAPSHOT"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.13-SNAPSHOT")

pomExtra :=
  <developers>
    <developer>
      <id>julienrf</id>
      <name>Julien Richard-Foy</name>
      <url>http://julien.richard-foy.fr</url>
    </developer>
  </developers>

homepage := Some(url(s"https://github.com/scalacenter/sbt-scalajs-bundler"))

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/scalacenter/sbt-scalajs-bundler"),
    "scm:git:git@github.com:scalacenter/sbt-scalajs-bundler.git"
  )
)

import ReleaseTransformations._
import xerial.sbt.Sonatype.SonatypeCommand

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  releaseStepInputTask(scripted),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepTask(PgpKeys.publishSigned),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand(SonatypeCommand.sonatypeReleaseAll),
  pushChanges
)

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts += "-Dplugin.version=" + version.value

scriptedBufferLog := false