package scalajsbundler.sbtplugin

import org.scalajs.core.tools.io.FileVirtualJSFile
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

import scalajsbundler.NpmDeps.NpmDeps
import scalajsbundler._

object NpmDepsPlugin extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  // Exported keys
  object autoImport {

    // From ScalaJSBundlerPlugin
    val npmUpdate = taskKey[File]("Fetch NPM dependencies")

    // From ScalaJSBundlerPlugin
    val useYarn: SettingKey[Boolean] =
      settingKey[Boolean]("Whether to use yarn for updates")

    // From ScalaJSBundlerPlugin
    val packageJson = TaskKey[BundlerFile.PackageJson]("packageJson",
      "Write a package.json file defining the NPM dependencies of project",
      KeyRanks.Invisible
    )

    val npmDeps = settingKey[NpmDeps]("List of js dependencies to be fetched")

    val allNpmDeps = taskKey[NpmDeps]("json file containing all npm js dependencies collected from all dependencies")

    val dependencyFile = taskKey[File]("File containing all external js files")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    npmDeps in Compile := List.empty,

    skip in packageJSDependencies := false,

    resolvedJSDependencies in Compile := {
      val logger = streams.value.log
      val prev = (resolvedJSDependencies in Compile).value

      // Fetch the js paths in node_modules
      val jss = {
        val nodeModules = (npmUpdate in Compile).value / "node_modules"

        (for {
          m <- (allNpmDeps in Compile).value
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

    dependencyFile := (packageMinifiedJSDependencies in Compile).value,

    (products in Compile) := (products in Compile).dependsOn(npmDepsManifest).value
  ) ++ perScalaJSStageSettings(fullOptJS) ++ perScalaJSStageSettings(fastOptJS)

  def perScalaJSStageSettings(stage: TaskKey[Attributed[File]]): Seq[Def.Setting[_]] = Seq(
    useYarn := false
  ) ++ inConfig(Compile)(perConfigSettings)


  private lazy val perConfigSettings: Seq[Def.Setting[_]] = Seq(
    allNpmDeps := NpmDeps.collectFromClasspath((fullClasspath in Compile).value),

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
      (allNpmDeps in Compile).value.map { dep => dep.module -> dep.version },
      Seq(),
      Map.empty,
      Map.empty,
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

  /**
    * Writes the NpmDeps manifest file.
    */
  val npmDepsManifest: Def.Initialize[Task[File]] =
    Def.task {
      scalajsbundler.NpmDeps.writeNpmDepsJson(
        (npmDeps in Compile).value,
        (classDirectory in Compile).value / NpmDeps.manifestFileName
      )
    }

}
