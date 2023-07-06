val scalaJSVersion = sys.env.getOrElse("SCALAJS_VERSION", "1.3.0")

lazy val `scalajs-bundler-linker` =
  project.in(file("scalajs-bundler-linker"))
    .settings(
      scalaVersion := "2.12.11",
      libraryDependencies += "org.scala-js" %% "scalajs-linker" % scalaJSVersion
    )

val `sbt-scalajs-bundler` =
  project.in(file("sbt-scalajs-bundler"))
    .enablePlugins(SbtPlugin, BuildInfoPlugin)
    .settings(commonSettings)
    .settings(
      description := "Module bundler for Scala.js projects",
      libraryDependencies += "com.google.jimfs" % "jimfs" % "1.3.0",
      libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.4",
      addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "scalajsbundler.sbtplugin.internal",
      // When supported, add: buildInfoOptions += sbtbuildinfo.BuildInfoOption.PackagePrivate
      scriptedDependencies := {
        val () = scriptedDependencies.value
        val () = publishLocal.value
        val () = (publishLocal in `scalajs-bundler-linker`).value
      },
    )

val `sbt-web-scalajs-bundler` =
  project.in(file("sbt-web-scalajs-bundler"))
    .enablePlugins(SbtPlugin)
    .settings(commonSettings)
    .settings(
      // sbt-web-scalajs does not support sbt 1.2.x
      crossSbtVersions := List("1.3.0"),
      sbtVersion in pluginCrossBuild := "1.3.0",
      scriptedDependencies := {
        val () = scriptedDependencies.value
        val () = publishLocal.value
        val () = (publishLocal in `sbt-scalajs-bundler`).value
        val () = (publishLocal in `scalajs-bundler-linker`).value
      },
      description := "Module bundler for Scala.js projects (integration with sbt-web-scalajs)",
      addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.1.0")
    )
    .dependsOn(`sbt-scalajs-bundler`)

// Dummy project that exists just for the purpose of aggregating the two sbt
// plugins. I can not do that in the `doc` project below because the
// scalaVersion is not compatible.
val apiDoc =
  project.in(file("api-doc"))
    .enablePlugins(ScalaUnidocPlugin)
    .settings(noPublishSettings: _*)
    .settings(
      scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
        "-groups",
        "-doc-source-url", s"https://github.com/scalacenter/scalajs-bundler/blob/v${version.value}€{FILE_PATH}.scala",
        "-sourcepath", (baseDirectory in ThisBuild).value.absolutePath
      ),
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(`scalajs-bundler-linker`)
    )
    .aggregate(`sbt-scalajs-bundler`, `sbt-web-scalajs-bundler`)

val ornateTarget = Def.setting(target.value / "ornate")

val manual =
  project.in(file("manual"))
    .enablePlugins(OrnatePlugin)
    .settings(noPublishSettings: _*)
    .settings(
      scalaVersion := "2.12.11",
      ornateSourceDir := Some(sourceDirectory.value / "ornate"),
      ornateTargetDir := Some(ornateTarget.value),
      ornateSettings := Map("version" -> version.value),
      siteSubdirName in ornate := "",
      addMappingsToSiteDir(mappings in ornate, siteSubdirName in ornate),
      mappings in ornate := {
        val _ = ornate.value
        val output = ornateTarget.value
        output ** AllPassFilter --- output pair Path.relativeTo(output)
      },
      siteSubdirName in packageDoc := "api/latest",
      addMappingsToSiteDir(mappings in ScalaUnidoc in packageDoc in apiDoc, siteSubdirName in packageDoc)
    )

val `scalajs-bundler` =
  project.in(file("."))
    .settings(noPublishSettings: _*)
    .aggregate(`sbt-scalajs-bundler`, `sbt-web-scalajs-bundler`)

inThisBuild(List(
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
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalacenter/scalajs-bundler"),
      "scm:git@github.com:scalacenter/scalajs-bundler.git"
    )
  ),
  organization := "ch.epfl.scala",
  homepage := Some(url(s"https://github.com/scalacenter/scalajs-bundler")),
  licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php")),
  developers := List(Developer("julienrf", "Julien Richard-Foy", "julien.richard-foy@epfl.ch", url("http://julien.richard-foy.fr")))
))

lazy val commonSettings = List(
  scriptedLaunchOpts ++= Seq(
    "-Dplugin.version=" + version.value,
    s"-Dscalajs.version=$scalaJSVersion",
    "-Dsbt.execute.extrachecks=true" // Avoid any deadlocks.
  ),
  scriptedBufferLog := false,
  crossSbtVersions := List("1.2.8"),
  sbtVersion in pluginCrossBuild := "1.2.8",
)

lazy val noPublishSettings =
  Seq(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
