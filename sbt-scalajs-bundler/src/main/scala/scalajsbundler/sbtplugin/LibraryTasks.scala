package scalajsbundler.sbtplugin

import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{
  scalaJSIR,
  scalaJSLinker,
  scalaJSLinkerConfig,
  scalaJSModuleInitializers,
  usesScalaJSLinkerTag
}
import sbt.Keys._
import sbt.{Def, _}

import scalajsbundler.{BundlerFile, Webpack, WebpackEntryPoint}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin._
import scalajsbundler.util.{Caching, JSBundler, ScalaJSOutputAnalyzer}

object LibraryTasks {
  private[sbtplugin] def bundle(
      stage: TaskKey[Attributed[File]],
      mode: BundlingMode.Library,
      entryPoint: SettingKey[File]): Def.Initialize[Task[BundlerFile.Library]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val log = streams.value.log
      val emitSourceMaps = (finallyEmitSourceMaps in stage).value
      val customWebpackConfigFile = (webpackConfigFile in stage).value
      val generatedWebpackConfigFile =
        (scalaJSBundlerWebpackConfig in stage).value
      val compileResources = (resources in Compile).value
      val webpackResourceFiles = (webpackResources in stage).value.get
      val entryPointFile = BundlerFile.EntryPoint(
        WebpackTasks.entry(stage).value, entryPoint.value)
      val monitoredFiles = customWebpackConfigFile.toSeq ++
        Seq(generatedWebpackConfigFile.file, entryPointFile.file) ++
        webpackResourceFiles ++ compileResources
      val cacheLocation = streams.value.cacheDirectory / s"${stage.key.label}-webpack-libraries"
      val extraArgs = (webpackExtraArgs in stage).value
      val nodeArgs = (webpackNodeArgs in stage).value
      val webpackMode = Webpack.WebpackMode((scalaJSLinkerConfig in stage).value)

      val cachedActionFunction =
        FileFunction.cached(
          cacheLocation,
          inStyle = FilesInfo.hash
        ) { _ =>
          log.info(s"Building webpack library bundles for ${entryPointFile.project} in $cacheLocation")

          Webpack.bundleLibraries(
            emitSourceMaps,
            generatedWebpackConfigFile,
            customWebpackConfigFile,
            webpackResourceFiles,
            entryPointFile,
            mode.exportedName,
            extraArgs,
            nodeArgs,
            webpackMode,
            log
          ).cached
        }
      val cached = cachedActionFunction(monitoredFiles.to[Set])
      generatedWebpackConfigFile.asLibraryFromCached(cached)
    }

  private[sbtplugin] def loader(
      stage: TaskKey[Attributed[File]],
      mode: BundlingMode.Library): Def.Initialize[Task[BundlerFile.Loader]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val entry = WebpackTasks.entry(stage).value
      val loaderFile = entry.asLoader

      JSBundler.writeLoader(
        loaderFile,
        mode.exportedName
      )
      loaderFile
    }

  private[sbtplugin] def bundleAll(stage: TaskKey[Attributed[File]],
                                  mode: BundlingMode.LibraryAndApplication,
                                  entryPoint: SettingKey[File])
    : Def.Initialize[Task[Seq[BundlerFile.Public]]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val cacheLocation = streams.value.cacheDirectory / s"${stage.key.label}-webpack-bundle-all"
      val targetDir = npmUpdate.value
      val entry = WebpackTasks.entry(stage).value
      val library = bundle(stage, mode, entryPoint).value
      val emitSourceMaps = (finallyEmitSourceMaps in stage).value
      val log = streams.value.log
      val filesToMonitor = Seq(entry.file, library.file)

      val cachedActionFunction =
        FileFunction.cached(
          cacheLocation,
          inStyle = FilesInfo.hash
        ) { _ =>
          JSBundler.bundle(
            targetDir = targetDir,
            entry,
            library,
            emitSourceMaps,
            mode.exportedName,
            log
          ).cached
        }
      val cached = cachedActionFunction(filesToMonitor.toSet)
      Seq(entry.asApplicationBundleFromCached(cached), entry.asLoader, library, entry)
    }

  private[sbtplugin] def librariesAndLoaders(stage: TaskKey[Attributed[File]],
                                             mode: BundlingMode.LibraryOnly,
                                             entryPoint: SettingKey[File])
    : Def.Initialize[Task[Seq[Attributed[File]]]] =
    Def.task {
      Seq(WebpackTasks.entry(stage).value,
        loader(stage, mode).value,
        bundle(stage, mode, entryPoint).value).flatMap(_.asAttributedFiles)
    }

  private[sbtplugin] def libraryAndLoadersBundle(
      stage: TaskKey[Attributed[File]],
      mode: BundlingMode.LibraryAndApplication,
      entryPoint: SettingKey[File])
    : Def.Initialize[Task[Seq[Attributed[File]]]] =
    Def.task {
      bundleAll(stage, mode, entryPoint).value.flatMap(_.asAttributedFiles)
    }
}
