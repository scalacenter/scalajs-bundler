import sbtunidoc.Plugin.ScalaUnidoc
import sbtunidoc.Plugin.UnidocKeys.unidoc

val `sbt-scalajs-bundler` =
  project.in(file("sbt-scalajs-bundler"))
    .settings(commonSettings: _*)
    .settings(
      sbtPlugin := true,
      name := "sbt-scalajs-bundler",
      description := "Module bundler for Scala.js projects",
      addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.18")
    )

val `sbt-web-scalajs-bundler` =
  project.in(file("sbt-web-scalajs-bundler"))
    .settings(commonSettings: _*)
    .settings(
      sbtPlugin := true,
      scriptedDependencies := {
        val () = scriptedDependencies.value
        val () = publishLocal.value
        val () = (publishLocal in `sbt-scalajs-bundler`).value
      },
      name := "sbt-web-scalajs-bundler",
      description := "Module bundler for Scala.js projects (integration with sbt-web-scalajs)",
      addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.5")
    )
    .dependsOn(`sbt-scalajs-bundler`)

// Dummy project that exists just for the purpose of aggregating the two sbt
// plugins. I can not do that in the `doc` project below because the
// scalaVersion is not compatible.
val apiDoc =
  project.in(file("api-doc"))
    .settings(noPublishSettings ++ unidocSettings: _*)
    .settings(
      scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
        "-groups",
        "-doc-source-url", s"https://github.com/scalacenter/scalajs-bundler/blob/v${version.value}€{FILE_PATH}.scala",
        "-sourcepath", (baseDirectory in ThisBuild).value.absolutePath
      )
    )
    .aggregate(`sbt-scalajs-bundler`, `sbt-web-scalajs-bundler`)

val ornateTarget = Def.setting(target.value / "ornate")

val manual =
  project.in(file("manual"))
    .enablePlugins(OrnatePlugin)
    .settings(noPublishSettings ++ ghpages.settings: _*)
    .settings(
      scalaVersion := "2.11.8",
      git.remoteRepo := "git@github.com:scalacenter/scalajs-bundler.git",
      ornateSourceDir := Some(sourceDirectory.value / "ornate"),
      ornateTargetDir := Some(ornateTarget.value),
      siteSubdirName in ornate := "",
      addMappingsToSiteDir(mappings in ornate, siteSubdirName in ornate),
      mappings in ornate := {
        val _ = ornate.value
        val output = ornateTarget.value
        output ** AllPassFilter --- output pair relativeTo(output)
      },
      siteSubdirName in packageDoc := "api/latest",
      addMappingsToSiteDir(mappings in ScalaUnidoc in packageDoc in apiDoc, siteSubdirName in packageDoc)
    )

import ReleaseTransformations._

val `scalajs-bundler` =
  project.in(file("."))
    .settings(ScriptedPlugin.scriptedSettings ++ noPublishSettings: _*)
    .settings(
      sbtPlugin := true,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        releaseStepInputTask(scripted in `sbt-scalajs-bundler`),
        releaseStepInputTask(scripted in `sbt-web-scalajs-bundler`),
        releaseStepTask(ornate in manual),
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        releaseStepTask(PgpKeys.publishSigned in `sbt-scalajs-bundler`),
        releaseStepTask(PgpKeys.publishSigned in `sbt-web-scalajs-bundler`),
        setNextVersion,
        commitNextVersion,
        pushChanges,
        releaseStepTask(GhPagesKeys.pushSite in manual)
      ),
      scriptedLaunchOpts += "-Dplugin.version=" + version.value,
      scriptedBufferLog := false
    )
    .aggregate(`sbt-scalajs-bundler`, `sbt-web-scalajs-bundler`, manual, apiDoc)

lazy val commonSettings =
  ScriptedPlugin.scriptedSettings ++ Seq(
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-encoding", "UTF-8",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture"
    ),
    organization := "ch.epfl.scala",
    developers ++= List(
      Developer(
        id = "julienrf",
        name = "Julien Richard-Foy",
        url = url("http://julien.richard-foy.fr"),
        email = "julien@richard-foy.fr"
      )
    ),
    homepage := Some(url(s"https://github.com/scalacenter/scalajs-bundler")),
    licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/scalacenter/scalajs-bundler"),
        "scm:git:git@github.com:scalacenter/scalajs-bundler.git"
      )
    ),
    scriptedLaunchOpts += "-Dplugin.version=" + version.value,
    scriptedBufferLog := false
  )

lazy val noPublishSettings =
  Seq(
    publishArtifact := false,
    publish := (),
    publishLocal := ()
  )
