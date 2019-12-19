package scalajsbundler.sbtplugin
import sbt.Keys._
import sbt.{Def, _}

import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSLinkerConfig
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import scalajsbundler.Webpack

object WebpackTasks {
  private[sbtplugin] def entry(stage: TaskKey[Attributed[File]])
  : Def.Initialize[Task[BundlerFile.Application]] =
    Def.task {
      val projectName = stage.value.data.name.stripSuffix(".js")
      BundlerFile.Application(projectName, stage.value.data, Nil)
    }

  private[sbtplugin] def webpack(
      stage: TaskKey[Attributed[File]]): Def.Initialize[Task[Seq[Attributed[File]]]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val cacheLocation = streams.value.cacheDirectory / s"${stage.key.label}-webpack"
      val generatedWebpackConfigFile =
        (scalaJSBundlerWebpackConfig in stage).value
      val emitSourceMaps = (finallyEmitSourceMaps in stage).value
      val customWebpackConfigFile = (webpackConfigFile in stage).value
      val webpackResourceFiles = webpackResources.value.get
      val entriesList = entry(stage).value
      val targetDir = npmUpdate.value
      val log = streams.value.log
      val monitoredFiles = (webpackMonitoredFiles in stage).value
      val extraArgs = (webpackExtraArgs in stage).value
      val nodeEnvVars = (webpackNodeEnvVars in stage).value
      val nodeArgs = (webpackNodeArgs in stage).value
      val webpackMode = Webpack.WebpackMode((scalaJSLinkerConfig in stage).value)

      val cachedActionFunction =
        FileFunction.cached(
          cacheLocation,
          inStyle = FilesInfo.hash
        ) { _ =>
          Webpack.bundle(
            emitSourceMaps,
            generatedWebpackConfigFile,
            customWebpackConfigFile,
            webpackResourceFiles,
            entriesList,
            targetDir,
            extraArgs,
            nodeEnvVars,
            nodeArgs,
            webpackMode,
            log
          ).cached
        }
      val cached = cachedActionFunction(monitoredFiles.to[Set])
      generatedWebpackConfigFile.asApplicationBundleFromCached(cached).asAttributedFiles
    }
}
