package scalajsbundler.sbtplugin

import org.scalajs.core.tools.io.FileVirtualJSFile
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
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

    case class Dep(module: String, version: String, jsFiles: String*)

    // From ScalaJSBundlerPlugin
    val npmUpdate = taskKey[File]("Fetch NPM dependencies")

    // From ScalaJSBundlerPlugin
    val useYarn: SettingKey[Boolean] =
      settingKey[Boolean]("Whether to use yarn for updates")

    // From ScalaJSBundlerPlugin
    val npmDependencies: SettingKey[Seq[(String, String)]] =
      settingKey[Seq[(String, String)]]("NPM dependencies (libraries that your program uses)")

    // From ScalaJSBundlerPlugin
    val npmResolutions: SettingKey[Map[String, String]] =
      settingKey[Map[String, String]]("NPM dependencies resolutions in case of conflict")

    // From ScalaJSBundlerPlugin
    val additionalNpmConfig: SettingKey[Map[String, JSON]] =
      settingKey[Map[String, JSON]]("Additional option to include in the generated 'package.json'")

    val npmDeps = settingKey[Seq[Dep]]("List of js depencies to be fetched")

    val dependencyFile = taskKey[File]("File containing all external js files")

    // Patched packageJson with no webpack reference
    val packageJson = TaskKey[BundlerFile.PackageJson]("packageJson",
      "Write a package.json file defining the NPM dependencies of project",
      KeyRanks.Invisible
    )

  }

  import autoImport._

  override lazy val projectSettings = Seq(
    npmDeps in Compile := Seq.empty,
    npmDependencies in Compile := Seq.empty,
    skip in packageJSDependencies := false,
    resolvedJSDependencies in Compile := {
      val logger = streams.value.log
      val prev = (resolvedJSDependencies in Compile).value

      // Fetch the js paths in node_modules
      val jss = {
        val nodeModules = (npmUpdate in Compile).value / "node_modules"

        (for {
          m <- (npmDeps in Compile).value
          js <- m.jsFiles
        } yield {
          logger.info(s"Fetch $js in ${nodeModules / m.module}")
          get(nodeModules / m.module, js)
        }).flatten

      }

      val resolvedDependencies = jss.map { f =>
        ResolvedJSDependency.minimal(FileVirtualJSFile(f))
      }

      prev.map(_ ++ resolvedDependencies)
    },
    dependencyFile := (packageMinifiedJSDependencies in Compile).value
  ) ++ perScalaJSStageSettings(fullOptJS) ++ perScalaJSStageSettings(fastOptJS)

  def perScalaJSStageSettings(stage: TaskKey[Attributed[File]]): Seq[Def.Setting[_]] = Seq(
    useYarn := false
  ) ++ inConfig(Compile)(perConfigSettings)


  private lazy val perConfigSettings: Seq[Def.Setting[_]] = Seq(
    npmResolutions := Map.empty,
    npmDependencies in Compile := {
      val prevNpmDep = (npmDependencies in Compile).value
      val newDeps = (npmDeps in Compile).value.map { dep => dep.module -> dep.version }
      prevNpmDep ++ newDeps
    },
    additionalNpmConfig := Map(
      "private" -> JSON.bool(true),
      "license" -> JSON.str("UNLICENSED")
    ),

    npmUpdate := {
      val log = streams.value.log
      val targetDir = (crossTarget in npmUpdate).value
      val jsResources = scalaJSNativeLibraries.value.data
      val packageJsonFile = packageJson.value

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

  private def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  private def get(path: File, jsFile: String) = {
    val files = recursiveListFiles(path)
    files.find(_.getName == jsFile)
  }

}
