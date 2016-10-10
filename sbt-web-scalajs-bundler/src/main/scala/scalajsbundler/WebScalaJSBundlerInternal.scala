package scalajsbundler

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline

import ScalaJSBundler.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import webscalajs.WebScalaJS
import webscalajs.WebScalaJS.autoImport._

object WebScalaJSBundlerInternal {

  val projectSettings = Seq(
    scalaJSDev := pipelineStage(fastOptJS in Compile, scalaJSDev).value,
    scalaJSProd := pipelineStage(fullOptJS in Compile, scalaJSProd).value
  )

  def pipelineStage(sjsStage: TaskKey[Attributed[File]], self: TaskKey[Pipeline.Stage]): Def.Initialize[Task[Pipeline.Stage]] =
    Def.taskDyn {
      val clients = scalaJSProjects.value.map(Project.projectToRef)
      Def.task { mappings: Seq[PathMapping] =>
        val bundles: Seq[(File, String)] =
          clients
            .map { client =>
              val task = webpack in(client, Compile, sjsStage in client)
              val clientTarget = crossTarget in (client, sjsStage)
              (task, clientTarget).map((files, target) => files pair relativeTo(target))
            }
            .foldLeft(Def.task(Seq.empty[(File, String)]))((acc, bundleFiles) => Def.task(acc.value ++ bundleFiles.value))
            .value
        val filtered = filterMappings(mappings, (includeFilter in self).value, (excludeFilter in self).value)
        filtered ++ bundles ++ WebScalaJS.sourcemapScalaFiles(sjsStage).value
      }
    }

  def filterMappings(mappings: Seq[PathMapping], include: FileFilter, exclude: FileFilter) =
    for ((file, path) <- mappings if include.accept(file) && !exclude.accept(file))
    yield file -> path

}
