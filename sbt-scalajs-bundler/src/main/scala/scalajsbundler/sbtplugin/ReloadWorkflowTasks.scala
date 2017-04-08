package scalajsbundler.sbtplugin

import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import org.scalajs.sbtplugin.ScalaJSPluginInternal
import org.scalajs.sbtplugin.ScalaJSPluginInternal.usesScalaJSLinkerTag
import sbt.Keys._
import sbt._

import scalajsbundler.ReloadWorkflow
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.ensureModuleKindIsCommonJSModule
import scalajsbundler.util.Caching.cached

/** Sbt tasks related to the reload workflow */
object ReloadWorkflowTasks {

  def webpackTask(config: Configuration, stage: TaskKey[Attributed[File]]): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val targetDir = (crossTarget in stage).value
      Seq(
        ReloadWorkflow.writeFakeBundle(
          (webpackEmitSourceMaps in stage).value,
          bundleDependenciesTask(config, stage).value,
          writeLoaderTask(stage).value,
          writeLauncherTask(stage).value,
          stage.value.data,
          targetDir,
          targetDir,
          streams.value.log
        )
      )
    }

  def bundleDependenciesTask(config: Configuration, stage: TaskKey[Attributed[File]]): Def.Initialize[Task[File]] =
    Def.taskDyn {
      val linker = (ScalaJSPluginInternal.scalaJSLinker in (config, stage)).value
      val linkerTag = (usesScalaJSLinkerTag in (config, stage)).value
      Def.task {
        val targetDir = (crossTarget in stage).value
        val logger = streams.value.log

        val entryPointFile = targetDir / "scalajsbundler-entry-point.js"
        val bundleFile = targetDir / "scalajsbundler-deps.js" // Don’t need to differentiate between stages because the dependencies should not be different between fastOptJS and fullOptJS
        val webpackCfgFile = (webpackConfigFile in webpackReload).value
        val webpackSharedCfgFiles = webpackSharedConfigFiles.value

        val importedModules =
          ReloadWorkflow.findImportedModules(
            linker,
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
            webpackCfgFile,
            webpackSharedCfgFiles,
            streams.value.log
          )
        }
        bundleFile
      }.tag(linkerTag).dependsOn(npmUpdate in stage)
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
