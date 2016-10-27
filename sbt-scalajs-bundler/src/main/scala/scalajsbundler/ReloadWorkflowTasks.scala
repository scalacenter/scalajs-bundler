package scalajsbundler

import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import org.scalajs.sbtplugin.ScalaJSPluginInternal
import sbt.Keys._
import sbt._
import ScalaJSBundlerPlugin.autoImport._

/** Sbt tasks related to the reload workflow */
object ReloadWorkflowTasks {

  def webpackTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      Seq(
        ReloadWorkflow.writeFakeBundle(
          bundleDependenciesTask(stage).value,
          writeLoaderTask(stage).value,
          writeLauncherTask(stage).value,
          stage.value.data,
          (crossTarget in stage).value
        )
      )
    }

  def bundleDependenciesTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[File]] =
    Def.task {
      val targetDir = (crossTarget in stage).value
      val logger = streams.value.log
      val entryPointFile = targetDir / "scalajsbundler-entry-point.js"
      val bundleFile = targetDir / "scalajsbundler-deps.js" // Don’t need to differentiate between stages because the dependencies should not be different between fastOptJS and fullOptJS
      val importedModules =
        ReloadWorkflow.findImportedModules(
          ScalaJSPluginInternal.scalaJSLinker.value,
          scalaJSIR.value.data,
          scalaJSOutputMode.value,
          emitSourceMaps.value,
          logger
        )
      cached(
        bundleFile,
        importedModules.##.toString,
        streams.value.cacheDirectory / "scalajsbundler-bundle"
      ) { () =>
        ReloadWorkflow.bundleDependencies(
          importedModules,
          targetDir,
          entryPointFile,
          bundleFile,
          streams.value.log
        )
      }
      bundleFile
    }.dependsOn(npmUpdate in stage)

  def writeLoaderTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[File]] =
    Def.task {
      val targetDir = (crossTarget in stage).value
      val loaderFile = targetDir / "scalajsbundler-deps-loader.js"
      if (!loaderFile.exists()) {
        ReloadWorkflow.writeLoader(loaderFile, streams.value.log)
      }
      loaderFile
    }

  def writeLauncherTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[File]] =
    Def.task {
      val entryPoint =
        (mainClass in (scalaJSLauncher in stage)).value.getOrElse("No main class detected")
      val targetDir = (crossTarget in stage).value
      val launcherFile = targetDir / s"scalajsbundler-${stage.key.label}-launcher.js"
      cached(
        launcherFile,
        entryPoint,
        streams.value.cacheDirectory / s"scalajsbundler-${stage.key.label}-launcher"
      ) { () =>
        ReloadWorkflow.writeLauncher(entryPoint, launcherFile, streams.value.log)
      }
      launcherFile
    }

  def cached(
    fileToWrite: File,
    hash: String,
    cache: File
  )(
    write: () => Unit
  ): Unit = {
    if (!fileToWrite.exists() || (cache.exists() && IO.read(cache) != hash)) {
      write()
      IO.write(cache, hash)
    }
  }

}
