package scalajsbundler

import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import org.scalajs.sbtplugin.ScalaJSPluginInternal
import sbt.Keys._
import sbt._
import ScalaJSBundlerPlugin.autoImport._

import scalajsbundler.ReloadWorkflow.LoaderAndLauncher

/** Sbt tasks related to the reload workflow */
object ReloadWorkflowTasks {

  def webpackTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      Seq(
        ReloadWorkflow.writeFakeBundle(
          bundleDependenciesTask(stage).value,
          writeLoaderAndLauncherTask(stage).value,
          stage.value.data,
          (crossTarget in stage).value
        )
      )
    }

  def bundleDependenciesTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[File]] =
    Def.task {
      ReloadWorkflow.bundleDependencies(
        ScalaJSPluginInternal.scalaJSLinker.value,
        scalaJSIR.value.data,
        scalaJSOutputMode.value,
        emitSourceMaps.value,
        (crossTarget in stage).value,
        streams.value.log
      )
    }.dependsOn(npmUpdate in stage)

  def writeLoaderAndLauncherTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[LoaderAndLauncher]] =
    Def.task {
      ReloadWorkflow.writeLoaderAndLauncher(
        (mainClass in (scalaJSLauncher in stage)).value.getOrElse("No main class detected"),
        (crossTarget in stage).value
      )
    }


}
