package scalajsbundler.sbtplugin

import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.Stage
import sbt.Keys._
import sbt._
import webscalajs.WebScalaJS
import webscalajs.WebScalaJS.autoImport._

import scala.collection.immutable.Nil
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

/** If WebScalaJS is enabled, tweaks the pipelineStage so that the bundle is produced as an sbt-web asset.
  *
  * =Tasks and Settings=
  *
  * See the [[WebScalaJSBundlerPlugin.autoImport autoImport]] member.
  */
object WebScalaJSBundlerPlugin extends AutoPlugin {

  /** @groupname settings Settings */
  object autoImport {

    /** Sequence of PathMapping’s to include to sbt-web’s assets.
      *
      * @see
      *   [[scalajsbundler.sbtplugin.NpmAssets.ofProject NpmAssets.ofProject]]
      * @group settings
      */
    val npmAssets: TaskKey[Seq[PathMapping]] =
      taskKey[Seq[PathMapping]]("Assets (resources that are not CommonJS modules) imported from the NPM packages")

    val NpmAssets = scalajsbundler.sbtplugin.NpmAssets

  }

  import autoImport._

  override lazy val requires = WebScalaJS

  override lazy val projectSettings = Seq(
    monitoredScalaJSDirectories ++= allFrontendProjectResourceDirectories.value,
    scalaJSPipeline := pipelineStage.value,
    npmAssets := Nil
  )

  val allFrontendProjectResourceDirectories: Def.Initialize[Seq[File]] = Def.settingDyn {
    val projectRefs = scalaJSProjects.value.map(Project.projectToRef)
    projectRefs
      .map { project =>
        Def.setting {
          (resourceDirectories in Compile in project).value
        }
      }
      .foldLeft(Def.setting(Seq.empty[File]))((acc, resourceDirectories) =>
        Def.setting(acc.value ++ resourceDirectories.value))
  }

  val bundlesWithSourceMaps: Def.Initialize[Task[Seq[(File, String)]]] =
    Def.settingDyn {
      val projects = scalaJSProjects.value.map(Project.projectToRef)
      Def.task {
        // ((file, relative-path), sourceMapsEnabled)
        val bundles: Seq[((File, String), Boolean)] =
          projects
            .map { project =>
              Def.settingDyn {
                val sjsStage = (scalaJSStage in project).value match {
                  case Stage.FastOpt => fastOptJS
                  case Stage.FullOpt => fullOptJS
                }
                Def.task {
                  val files = (webpack in (project, Compile, sjsStage)).value
                  val clientTarget = (npmUpdate in (project, Compile)).value
                  val sourceMapsEnabled = (finallyEmitSourceMaps in (project, Compile, sjsStage)).value
                  files.map(_.data).pair(Path.relativeTo(clientTarget)).map((_, sourceMapsEnabled))
                }
              }
            }
            .foldLeft(Def.task(Seq.empty[((File, String), Boolean)]))((acc, bundleFiles) =>
              Def.task(acc.value ++ bundleFiles.value))
            .value

        bundles.flatMap { case ((file, path), bundleSourceMapsEnabled) =>
          val sourceMapFile = file.getParentFile / (file.name ++ ".map")
          if (bundleSourceMapsEnabled && sourceMapFile.exists) {
            val sourceMapPath = path ++ ".map"
            Seq(file -> path, sourceMapFile -> sourceMapPath)
          } else Seq(file -> path)
        }
      }
    }

  val pipelineStage: Def.Initialize[Task[Pipeline.Stage]] =
    Def.taskDyn {
      val npmAssetsMappings = npmAssets.value
      val include = (includeFilter in scalaJSPipeline).value
      val exclude = (excludeFilter in scalaJSPipeline).value
      val bundleMappings = bundlesWithSourceMaps.value
      val sourcemapScalaFiles = WebScalaJS.sourcemapScalaFiles.value
      Def.task { mappings: Seq[PathMapping] =>
        val filtered = filterMappings(mappings, include, exclude)

        filtered ++ bundleMappings ++ sourcemapScalaFiles ++ npmAssetsMappings
      }
    }

  def filterMappings(mappings: Seq[PathMapping], include: FileFilter, exclude: FileFilter): Seq[PathMapping] =
    for {
      (file, path) <- mappings
      if include.accept(file) && !exclude.accept(file)
    } yield file -> path

}
