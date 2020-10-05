val runScripted = taskKey[Unit]("Run supported sbt scripted tests")

val scalaJSVersion = sys.env.getOrElse("SCALAJS_VERSION", "1.0.0")
val isScalaJS1x = scalaJSVersion.startsWith("1.")
val scalaJSSourceDirectorySuffix = if (isScalaJS1x) "sjs-1.x" else "sjs-0.6"

// This project is only used with Scala.js 1.x
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
      name := (if (isScalaJS1x) "sbt-scalajs-bundler" else "sbt-scalajs-bundler-sjs06"),
      description := "Module bundler for Scala.js projects",
      libraryDependencies += "com.google.jimfs" % "jimfs" % "1.1",
      libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.14",
      addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
      unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / s"scala-$scalaJSSourceDirectorySuffix",
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "scalajsbundler.sbtplugin.internal",
      // When supported, add: buildInfoOptions += sbtbuildinfo.BuildInfoOption.PackagePrivate
      if (isScalaJS1x) {
        scriptedDependencies := {
          val () = scriptedDependencies.value
          val () = publishLocal.value
          val () = (publishLocal in `scalajs-bundler-linker`).value
        }
      } else {
        scriptedDependencies := {
          val () = scriptedDependencies.value
          val () = publishLocal.value
        }
      }
    )

val `sbt-web-scalajs-bundler` =
  project.in(file("sbt-web-scalajs-bundler"))
    .enablePlugins(SbtPlugin)
    .settings(commonSettings)
    .settings(
      // sbt-web-scalajs does not support sbt 1.2.x
      crossSbtVersions := List("1.3.0"),
      sbtVersion in pluginCrossBuild := "1.3.0",

      if (isScalaJS1x) {
        scriptedDependencies := {
          val () = scriptedDependencies.value
          val () = publishLocal.value
          val () = (publishLocal in `sbt-scalajs-bundler`).value
          val () = (publishLocal in `scalajs-bundler-linker`).value
        }
      } else {
        scriptedDependencies := {
          val () = scriptedDependencies.value
          val () = publishLocal.value
          val () = (publishLocal in `sbt-scalajs-bundler`).value
        }
      },
      name := (if (isScalaJS1x) "sbt-web-scalajs-bundler" else "sbt-web-scalajs-bundler-sjs06"),
      description := "Module bundler for Scala.js projects (integration with sbt-web-scalajs)",
      addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % (if (isScalaJS1x) "1.1.0" else "1.1.0-0.6"))
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
    .enablePlugins(OrnatePlugin, GhpagesPlugin)
    .settings(noPublishSettings: _*)
    .settings(
      scalaVersion := "2.12.11",
      git.remoteRepo := "git@github.com:scalacenter/scalajs-bundler.git",
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
  pgpPublicRing := file("./travis/local.pubring.asc"),
  pgpSecretRing := file("./travis/local.secring.asc"),
  pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray),
  credentials ++= (
    for {
      username <- sys.env.get("SONATYPE_USER")
      password <- sys.env.get("SONATYPE_PASSWORD")
    } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)
  ).toList,
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
  runScripted := runScriptedTask.value,
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

// Run all the sbt-scripted tests that are compatible with both the selected sbt version
// and the selected Scala.js version
def runScriptedTask = Def.taskDyn {
  val sbtBinVersion = (sbtBinaryVersion in pluginCrossBuild).value
  val base = sbtTestDirectory.value

  def isCompatible(directory: File): Boolean = {
    val buildProps = new java.util.Properties()
    IO.load(buildProps, directory / "project" / "build.properties")
    val sbtIncompatibility =
      Option(buildProps.getProperty("sbt.version"))
      .flatMap { version =>
        val requiredBinVersion = CrossVersion.binarySbtVersion(version)
        if (requiredBinVersion == sbtBinVersion) None
        else Some(s"it requires sbt $requiredBinVersion")
      }
    val scalaJSIncompatibility =
      if (directory.name.endsWith("_sjs-0.6") && !scalaJSVersion.startsWith("0.6")) Some(s"it requires Scala.js 0.6")
      else if (directory.name.endsWith("_sjs-1") && !scalaJSVersion.startsWith("1.")) Some("it requires Scala.js 1.x")
      else None

    sbtIncompatibility.orElse(scalaJSIncompatibility) match {
      case Some(reason) =>
        val testName = directory.relativeTo(base).getOrElse(directory)
        streams.value.log.info(s"Skipping $testName since $reason")
        false
      case None =>
        true
    }
  }

  val testDirectoryFinder = base * AllPassFilter * AllPassFilter filter { _.isDirectory }
  val tests = for {
    test <- testDirectoryFinder.get
    if isCompatible(test)
    path <- Path.relativeTo(base)(test)
  } yield path.replace('\\', '/')

  if (tests.nonEmpty)
    Def.task(scripted.toTask(tests.mkString(" ", " ", "")).value)
  else
    Def.task(streams.value.log.warn("No tests can be run for this sbt version"))
}

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
