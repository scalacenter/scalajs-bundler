package scalajsbundler.sbtplugin

import org.scalajs.core.tools.io.FileVirtualJSFile
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

import scalajsbundler.util.JSON
import scalajsbundler.{BundlerFile, Npm, Yarn}

object NpmDepsPlugin extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  // Exported keys
  object autoImport {

    val npmUpdate = taskKey[File]("Fetch NPM dependencies")

    val useYarn: SettingKey[Boolean] =
      settingKey[Boolean]("Whether to use yarn for updates")

    /**
      * List of the NPM packages (name and version) your application depends on.
      * You can use [semver](https://docs.npmjs.com/misc/semver) versions:
      *
      * {{{
      *   npmDependencies in Compile += "uuid" -> "~3.0.0"
      * }}}
      *
      * Note that this key must be scoped by a `Configuration` (either `Compile` or `Test`).
      *
      * @group settings
      */
    val npmDependencies: SettingKey[Seq[(String, String)]] =
      settingKey[Seq[(String, String)]]("NPM dependencies (libraries that your program uses)")

    val packageJson = TaskKey[BundlerFile.PackageJson]("packageJson",
      "Write a package.json file defining the NPM dependencies of project",
      KeyRanks.Invisible
    )

    val npmResolutions: SettingKey[Map[String, String]] =
      settingKey[Map[String, String]]("NPM dependencies resolutions in case of conflict")

    val additionalNpmConfig: SettingKey[Map[String, JSON]] =
      settingKey[Map[String, JSON]]("Additional option to include in the generated 'package.json'")

  }

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    npmDependencies := Seq.empty
  ) ++ perScalaJSStageSettings(fullOptJS) ++ perScalaJSStageSettings(fastOptJS)

  def perScalaJSStageSettings(stage: TaskKey[Attributed[File]]): Seq[Def.Setting[_]] = Seq(
    useYarn := false
  ) ++ inConfig(Compile)(perConfigSettings)


  private lazy val perConfigSettings: Seq[Def.Setting[_]] = Seq(
    npmResolutions := Map.empty,

    additionalNpmConfig := Map(
      "private" -> JSON.bool(true),
      "license" -> JSON.str("UNLICENSED")
    ),

    npmUpdate := {
      println("NPM UPDATE NPM DEPS")
      val log = streams.value.log
      val targetDir = (crossTarget in npmUpdate).value
      val jsResources = scalaJSNativeLibraries.value.data
      val packageJsonFile = packageJson.value

      println("packJSON " + packageJsonFile.file.getAbsolutePath)

      val cachedActionFunction =
        FileFunction.cached(
          streams.value.cacheDirectory / "scalajsbundler-npm-update",
          inStyle = FilesInfo.hash
        ) { _ =>
          log.info("Updating NPM dependencies")
          if (useYarn.value) {
            Yarn.run("install", "--non-interactive")(targetDir, log)
          } else {
            Npm.run("install")(targetDir, log)
          }
          jsResources.foreach { resource =>
            IO.write(targetDir / resource.relativePath, resource.content)
          }
          Set.empty
        }

      cachedActionFunction(Set(packageJsonFile.file) ++
        jsResources.collect { case f: FileVirtualJSFile =>
          println("in cached " + f.file.getAbsolutePath)
          f.file
        }.to[Set])

      targetDir
    },

    packageJson := PackageJsonTasks.writePackageJson(
      (crossTarget in npmUpdate).value,
      npmDependencies.value,
      Seq(),
      npmResolutions.value,
      additionalNpmConfig.value,
      fullClasspath.value,
      configuration.value,
      "3",
      "2.7.1",
      streams.value
    )

  )

}
