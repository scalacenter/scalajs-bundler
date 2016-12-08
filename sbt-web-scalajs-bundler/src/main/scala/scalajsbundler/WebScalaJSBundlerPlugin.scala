package scalajsbundler

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline

import ScalaJSBundlerPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import webscalajs.WebScalaJS
import webscalajs.WebScalaJS.autoImport._

import scala.collection.immutable.Nil

/**
  * If WebScalaJS is enabled, tweak the pipelineStage so that the bundle is produced
  * as an sbt-web asset.
  */
object WebScalaJSBundlerPlugin extends AutoPlugin {

  object autoImport {

    /** @see [[scalajsbundler.NpmAssets$.ofProject NpmAssets.ofProject]] */
    val npmAssets: TaskKey[Seq[PathMapping]] = taskKey[Seq[PathMapping]]("Assets (resources that are not CommonJS modules) imported from the NPM packages")

    val NpmAssets = scalajsbundler.NpmAssets

  }

  import autoImport._

  override lazy val requires = WebScalaJS

  override lazy val projectSettings = Seq(
    scalaJSDev := pipelineStage(fastOptJS in Compile, scalaJSDev).value,
    scalaJSProd := pipelineStage(fullOptJS in Compile, scalaJSProd).value,
    npmAssets := Nil
  )

  def pipelineStage(sjsStage: TaskKey[Attributed[File]], self: TaskKey[Pipeline.Stage]): Def.Initialize[Task[Pipeline.Stage]] =
    Def.taskDyn {
      val projects = scalaJSProjects.value.map(Project.projectToRef)
      val npmAssetsMappings = npmAssets.value
      Def.task { mappings: Seq[PathMapping] =>
        val filtered = filterMappings(mappings, (includeFilter in self).value, (excludeFilter in self).value)

        // ((file, relative-path), sourceMapsEnabled)
        val bundles: Seq[((File, String), Boolean)] =
          projects
            .map { project =>
              val task = webpack in (project, Compile, sjsStage in project)
              val clientTarget = crossTarget in (project, sjsStage)
              val sourceMapsEnabled = webpackEmitSourceMaps in (project, Compile, sjsStage in project)
              (task, clientTarget, sourceMapsEnabled).map((files, target, enabled) => files.pair(relativeTo(target)).map((_, enabled)))
            }
            .foldLeft(Def.task(Seq.empty[((File, String), Boolean)]))((acc, bundleFiles) => Def.task(acc.value ++ bundleFiles.value))
            .value

        val bundlesWithSourceMaps =
          bundles.flatMap { case ((file, path), sourceMapsEnabled) =>
            if (sourceMapsEnabled) {
              val sourceMapFile = file.getParentFile / (file.name ++ ".map")
              val sourceMapPath = path ++ ".map"
              Seq(file -> path, sourceMapFile -> sourceMapPath)
            } else Seq(file -> path)
          }

        filtered ++ bundlesWithSourceMaps ++ WebScalaJS.sourcemapScalaFiles(sjsStage).value ++ npmAssetsMappings
      }
    }

  def filterMappings(mappings: Seq[PathMapping], include: FileFilter, exclude: FileFilter): Seq[PathMapping] =
    for {
      (file, path) <- mappings
      if include.accept(file) && !exclude.accept(file)
    } yield file -> path

}
