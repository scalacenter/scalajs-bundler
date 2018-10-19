import sbtunidoc.Plugin.ScalaUnidoc
import sbtunidoc.Plugin.UnidocKeys.unidoc

val runScripted = taskKey[Unit]("Run supported sbt scripted tests")

val `sbt-scalajs-bundler` =
  project.in(file("sbt-scalajs-bundler"))
    .settings(commonSettings)
    .settings(
      sbtPlugin := true,
      name := "sbt-scalajs-bundler",
      description := "Module bundler for Scala.js projects",
      libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.7",
      addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")
    )

val `sbt-web-scalajs-bundler` =
  project.in(file("sbt-web-scalajs-bundler"))
    .settings(commonSettings)
    .settings(
      sbtPlugin := true,
      scriptedDependencies := {
        val () = scriptedDependencies.value
        val () = publishLocal.value
        val () = (publishLocal in `sbt-scalajs-bundler`).value
      },
      name := "sbt-web-scalajs-bundler",
      description := "Module bundler for Scala.js projects (integration with sbt-web-scalajs)",
      addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.6")
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
      scalaVersion := "2.11.12",
      git.remoteRepo := "git@github.com:scalacenter/scalajs-bundler.git",
      ornateSourceDir := Some(sourceDirectory.value / "ornate"),
      ornateTargetDir := Some(ornateTarget.value),
      ornateSettings := Map("version" -> version.value),
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

val `scalajs-bundler` =
  project.in(file("."))
    .settings(noPublishSettings: _*)
    .aggregate(`sbt-scalajs-bundler`, `sbt-web-scalajs-bundler`)

inScope(ThisScope.copy(project = Global))(List(
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

lazy val commonSettings = ScriptedPlugin.scriptedSettings ++ List(
  runScripted := runScriptedTask.value,
  scriptedLaunchOpts += "-Dplugin.version=" + version.value,
  scriptedBufferLog := false,
  crossSbtVersions := List("0.13.17", "1.0.2"),
  scalaVersion := {
    (sbtBinaryVersion in pluginCrossBuild).value match {
      case "0.13" => "2.10.7"
      case _ => "2.12.3"
    }
  },
  // fixed in https://github.com/sbt/sbt/pull/3397 (for sbt 0.13.17)
  sbtBinaryVersion in update := (sbtBinaryVersion in pluginCrossBuild).value
)

lazy val noPublishSettings =
  Seq(
    publishArtifact := false,
    publish := (),
    publishLocal := ()
  )

def runScriptedTask = Def.taskDyn {
  val sbtBinVersion = (sbtBinaryVersion in pluginCrossBuild).value
  val base = sbtTestDirectory.value

  def isCompatible(directory: File): Boolean = {
    val buildProps = new java.util.Properties()
    IO.load(buildProps, directory / "project" / "build.properties")
    Option(buildProps.getProperty("sbt.version"))
      .map { version =>
        val requiredBinVersion = CrossVersion.binarySbtVersion(version)
        val compatible = requiredBinVersion == sbtBinVersion
        if (!compatible) {
          val testName = directory.relativeTo(base).getOrElse(directory)
          streams.value.log.warn(s"Skipping $testName since it requires sbt $requiredBinVersion")
        }
        compatible
      }
      .getOrElse(true)
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
