package scalajsbundler

import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import org.scalajs.sbtplugin.ScalaJSPluginInternal
import org.scalajs.sbtplugin.ScalaJSPluginInternal.usesScalaJSLinkerTag
import sbt.Keys._
import sbt._
import ScalaJSBundlerPlugin.autoImport._
import ScalaJSBundlerPlugin.ensureModuleKindIsCommonJSModule
import Caching.cached

/** Sbt tasks related to the reload workflow */
object ReloadWorkflowTasks {

  def webpackTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val targetDir = (crossTarget in stage).value
      Seq(
        ReloadWorkflow.writeFakeBundle(
          (webpackEmitSourceMaps in stage).value,
          bundleDependenciesTask(stage).value,
          writeLoaderTask(stage).value,
          writeLauncherTask(stage).value,
          stage.value.data,
          targetDir,
          targetDir,
          streams.value.log
        )
      )
    }

  def bundleDependenciesTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[File]] =
    Def.taskDyn {
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
            (emitSourceMaps in stage).value,
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
      }.dependsOn(npmUpdate in stage).tag((usesScalaJSLinkerTag in stage).value)
    }

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
        (mainClass in (scalaJSLauncher in stage)).value.getOrElse(sys.error("No main class detected"))
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

}
