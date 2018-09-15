package scalajsbundler.sbtplugin

import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.{
  scalaJSIR,
  scalaJSLinkerConfig,
  scalaJSModuleInitializers
}
import org.scalajs.sbtplugin.ScalaJSPluginInternal.{
  scalaJSLinker,
  usesScalaJSLinkerTag
}
import sbt.Keys._
import sbt.{Def, _}

import scalajsbundler.{BundlerFile, Webpack, WebpackEntryPoint}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin._
import scalajsbundler.util.{Caching, JSBundler, ScalaJSOutputAnalyzer}

object LibraryTasks {

  private[sbtplugin] def entryPoint(stage: TaskKey[Attributed[File]])
    : Def.Initialize[Task[BundlerFile.EntryPoint]] =
    Def.taskDyn {
      val s = streams.value
      val linkerConfig = (scalaJSLinkerConfig in stage).value
      val linker = (scalaJSLinker in stage).value
      val linkerTag = (usesScalaJSLinkerTag in stage).value
      val entry = WebpackTasks.entry(stage).value
      val scalaJSIRValue = scalaJSIR.value
      val scalaJSModuleInitializersValue = scalaJSModuleInitializers.value
      val cacheLocation = streams.value.cacheDirectory / s"${stage.key.label}-webpack-entrypoint"
      Def
        .task {
          val entryPointFile = entry.asEntryPoint
          val linkingUnit = ScalaJSOutputAnalyzer.linkingUnit(
            linkerConfig,
            linker,
            scalaJSIRValue.data,
            scalaJSModuleInitializersValue,
            s.log
          )
          val importedModules =
            ScalaJSOutputAnalyzer.importedModules(linkingUnit)

          // Avoid re-writing the entrypoint file if the list of modules hasn't changed
          // allowing downstream caching to detect change reliably
          Caching.cached(entryPointFile.file,
                         importedModules.mkString(","),
                         cacheLocation)(
            () =>
              WebpackEntryPoint.writeEntryPoint(
                importedModules,
                entryPointFile,
                s.log
            ))
          entryPointFile
        }
        .tag(linkerTag)
        .dependsOn(npmUpdate in stage)
    }

  private[sbtplugin] def bundle(
      stage: TaskKey[Attributed[File]],
      mode: BundlingMode.Library): Def.Initialize[Task[BundlerFile.Library]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val log = streams.value.log
      val emitSourceMaps = (webpackEmitSourceMaps in stage).value
      val customWebpackConfigFile = (webpackConfigFile in stage).value
      val generatedWebpackConfigFile =
        (scalaJSBundlerWebpackConfig in stage).value
      val compileResources = (resources in Compile).value
      val webpackResourceFiles = (webpackResources in stage).value.get
      val entryPointFile = entryPoint(stage).value
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
                                   mode: BundlingMode.LibraryAndApplication)
    : Def.Initialize[Task[Seq[BundlerFile.Public]]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val cacheLocation = streams.value.cacheDirectory / s"${stage.key.label}-webpack-bundle-all"
      val targetDir = npmUpdate.value
      val entry = WebpackTasks.entry(stage).value
      val library = bundle(stage, mode).value
      val emitSourceMaps = (webpackEmitSourceMaps in stage).value
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
                                             mode: BundlingMode.LibraryOnly)
    : Def.Initialize[Task[Seq[Attributed[File]]]] =
    Def.task {
      Seq(WebpackTasks.entry(stage).value,
        loader(stage, mode).value,
        bundle(stage, mode).value).flatMap(_.asAttributedFiles)
    }

  private[sbtplugin] def libraryAndLoadersBundle(
      stage: TaskKey[Attributed[File]],
      mode: BundlingMode.LibraryAndApplication)
    : Def.Initialize[Task[Seq[Attributed[File]]]] =
    Def.task {
      bundleAll(stage, mode).value.flatMap(_.asAttributedFiles)
    }
}
