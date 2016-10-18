val `sbt-scalajs-bundler` =
  project.in(file("sbt-scalajs-bundler"))
    .settings(commonSettings: _*)
    .settings(
      sbtPlugin := true,
      name := "sbt-scalajs-bundler",
      description := "Module bundler for Scala.js projects",
      addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.13")
    )

val `sbt-web-scalajs-bundler` =
  project.in(file("sbt-web-scalajs-bundler"))
    .settings(commonSettings: _*)
    .settings(
      sbtPlugin := true,
      name := "sbt-web-scalajs-bundler",
      description := "Module bundler for Scala.js projects (integration with sbt-web-scalajs)",
      addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.2")
    )
    .dependsOn(`sbt-scalajs-bundler`)

val ornateTarget = Def.setting(target.value / "ornate")

val doc =
  project.in(file("doc"))
    .enablePlugins(SiteScaladocPlugin, OrnatePlugin)
    .settings(noPublishSettings ++ ghpages.settings: _*)
    .settings(
      scalaVersion := "2.11.8",
      resolvers += Resolver.bintrayRepo("szeiger", "maven"), // https://github.com/szeiger/ornate/issues/4
      git.remoteRepo := "git@github.com:scalacenter/scalajs-bundler.git",
      ornateTargetDir := Some(ornateTarget.value),
      siteSourceDirectory := ornateTarget.value,
      includeFilter in makeSite := AllPassFilter,
      makeSite := makeSite.dependsOn(ornate).value
    )

import ReleaseTransformations._
import xerial.sbt.Sonatype.SonatypeCommand

val `scalajs-bundler` =
  project.in(file("."))
    .settings(ScriptedPlugin.scriptedSettings ++ noPublishSettings: _*)
    .settings(
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        releaseStepInputTask(scripted),
        releaseStepTask(ornate in doc),
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        releaseStepTask(PgpKeys.publishSigned),
        setNextVersion,
        commitNextVersion,
        releaseStepCommand(SonatypeCommand.sonatypeReleaseAll),
        pushChanges,
        releaseStepTask(GhPagesKeys.pushSite in doc)
      ),
      scriptedLaunchOpts += "-Dplugin.version=" + version.value,
      scriptedBufferLog := false
    )
    .aggregate(`sbt-scalajs-bundler`, `sbt-web-scalajs-bundler`, doc)

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
    pomExtra :=
      <developers>
        <developer>
          <id>julienrf</id>
          <name>Julien Richard-Foy</name>
          <url>http://julien.richard-foy.fr</url>
        </developer>
      </developers>,
    homepage := Some(url(s"https://github.com/scalacenter/sbt-scalajs-bundler")),
    licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/scalacenter/sbt-scalajs-bundler"),
        "scm:git:git@github.com:scalacenter/sbt-scalajs-bundler.git"
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