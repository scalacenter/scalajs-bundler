package scalajsbundler

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline

import ScalaJSBundlerPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import webscalajs.WebScalaJS
import webscalajs.WebScalaJS.autoImport._

/**
  * If WebScalaJS is enabled, tweak the pipelineStage so that the bundle is produced
  * as an sbt-web asset.
  */
object WebScalaJSBundlerPlugin extends AutoPlugin {

  override lazy val requires = WebScalaJS

  override lazy val projectSettings = Seq(
    scalaJSDev := pipelineStage(fastOptJS in Compile, scalaJSDev).value,
    scalaJSProd := pipelineStage(fullOptJS in Compile, scalaJSProd).value
  )

  def pipelineStage(sjsStage: TaskKey[Attributed[File]], self: TaskKey[Pipeline.Stage]): Def.Initialize[Task[Pipeline.Stage]] =
    Def.taskDyn {
      val projects = scalaJSProjects.value.map(Project.projectToRef)
      Def.task { mappings: Seq[PathMapping] =>
        val bundles: Seq[(File, String)] =
          projects
            .map { project =>
              val task = webpack in (project, Compile, sjsStage in project)
              val clientTarget = crossTarget in (project, sjsStage)
              (task, clientTarget).map((files, target) => files pair relativeTo(target))
            }
            .foldLeft(Def.task(Seq.empty[(File, String)]))((acc, bundleFiles) => Def.task(acc.value ++ bundleFiles.value))
            .value
        val filtered = filterMappings(mappings, (includeFilter in self).value, (excludeFilter in self).value)
        val bundlesWithSourceMaps =
          bundles.flatMap { case (file, path) =>
            val sourceMapFile = file.getParentFile / (file.name ++ ".map")
            val sourceMapPath = path ++ ".map"
            Seq(file -> path, sourceMapFile -> sourceMapPath)
          }
        filtered ++ bundlesWithSourceMaps ++ WebScalaJS.sourcemapScalaFiles(sjsStage).value
      }
    }

  def filterMappings(mappings: Seq[PathMapping], include: FileFilter, exclude: FileFilter) =
    for {
      (file, path) <- mappings
      if include.accept(file) && !exclude.accept(file)
    } yield file -> path

}
