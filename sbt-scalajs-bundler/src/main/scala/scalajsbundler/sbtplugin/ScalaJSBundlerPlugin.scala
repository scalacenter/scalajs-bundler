package scalajsbundler.sbtplugin

import java.util.concurrent.atomic.AtomicReference

import org.scalajs.core.tools.io.FileVirtualJSFile
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.core.tools.linker.backend.ModuleKind
import org.scalajs.jsenv.ComJSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.sbtplugin.Loggers.sbtLogger2ToolsLogger
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPluginInternal._
import org.scalajs.sbtplugin.{ScalaJSPlugin, ScalaJSPluginInternal, Stage}
import org.scalajs.testadapter.TestAdapter
import sbt.Keys._
import sbt._

import scala.annotation.tailrec
import scalajsbundler.ExternalCommand.install
import scalajsbundler._
import scalajsbundler.util.JSON

/**
  * This plugin enables `ScalaJSPlugin` and sets the `scalaJSModuleKind` to `CommonJSModule`. It also makes it
  * possible to define dependencies to NPM packages and provides tasks to fetch them or to bundle the application
  * with its dependencies.
  *
  * = Tasks and Settings =
  *
  * The [[ScalaJSBundlerPlugin.autoImport autoImport]] member documents the keys provided by this plugin. Besides these keys, the
  * following existing keys also control the plugin:
  *
  * == `version in webpack` ==
  *
  * Version of webpack to use. Example:
  *
  * {{{
  *   version in webpack := "3.5.5"
  * }}}
  *
  * == `version in installJsdom` ==
  *
  * Version of jsdom to use.
  *
  * == `version in startWebpackDevServer` ==
  *
  * Version of webpack-dev-server to use.
  *
  * {{{
  *   version in startWebpackDevServer := "2.7.1"
  * }}}
  *
  * == `crossTarget in npmUpdate` ==
  *
  * The directory in which NPM dependencies will be fetched, and where all the .js files
  * will be generated. The directory is different according to the current `Configuration`
  * (either `Compile` or `Test`).
  *
  * Defaults to `crossTarget.value / "scalajs-bundler" / "main"` for `Compile` and
  * `crossTarget.value / "scalajs-bundler" / "test"` for `Test`.
  */
object ScalaJSBundlerPlugin extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  // Exported keys
  /**
    * @groupname tasks Tasks
    * @groupname settings Settings
    */
  object autoImport {
    type BundlingMode = scalajsbundler.BundlingMode
    val BundlingMode = scalajsbundler.BundlingMode
    type BundlerFile = scalajsbundler.BundlerFile
    val BundlerFile = scalajsbundler.BundlerFile
    type BundlerFileType = scalajsbundler.BundlerFileType
    val BundlerFileType = scalajsbundler.BundlerFileType
    val ProjectNameAttr: AttributeKey[String] = SBTBundlerFile.ProjectNameAttr
    val BundlerFileTypeAttr: AttributeKey[BundlerFileType] = SBTBundlerFile.BundlerFileTypeAttr
    implicit class RichBundlerFile(f: BundlerFile.Public) extends SBTBundlerFile(f)

    /**
      * Fetches NPM dependencies. Returns the directory in which the `npm install` command has been run.
      *
      * The plugin uses different directories according to the configuration (`Compile` or `Test`). Thus,
      * this setting is scoped by a `Configuration`.
      *
      * Typically, if you want to define a task that uses the downloaded NPM packages you should
      * specify the `Configuration` you are interested in:
      *
      * {{{
      *   myCustomTask := {
      *     val npmDirectory = (npmUpdate in Compile).value
      *     doSomething(npmDirectory / "node_modules" / "some-package")
      *   }
      * }}}
      *
      * The task returns the directory in which the dependencies have been fetched (the directory
      * that contains the `node_modules` directory).
      *
      * @group tasks
      */
    val npmUpdate: TaskKey[File] =
      taskKey[File]("Fetch NPM dependencies")

    /**
      * List of the NPM packages (name and version) your application depends on.
      * You can use [semver](https://docs.npmjs.com/misc/semver) versions:
      *
      * {{{
      *   npmDependencies in Compile += "uuid" -> "~3.0.0"
      * }}}
      *
      * Note that this key must be scoped by a `Configuration` (either `Compile` or `Test`).
      *
      * @group settings
      */
    val npmDependencies: SettingKey[Seq[(String, String)]] =
      settingKey[Seq[(String, String)]]("NPM dependencies (libraries that your program uses)")

    /** @group settings */
    val npmDevDependencies: SettingKey[Seq[(String, String)]] =
      settingKey[Seq[(String, String)]]("NPM dev dependencies (libraries that the build uses)")

    /**
      * Map of NPM packages (name -> version) to use in case transitive NPM dependencies
      * refer to a same package but with different version numbers. In such a
      * case, this setting defines which version should be used for the conflicting
      * package. Example:
      *
      * {{{
      *   npmResolutions in Compile := Map("react" -> "15.4.1")
      * }}}
      *
      * If several Scala.js projects depend on different versions of `react`, the version `15.4.1`
      * will be picked. But if all the projects depend on the same version of `react`, the version
      * given in `npmResolutions` will be ignored.
      *
      * If different versions of the packages are referred but the package is NOT configured in `npmResolutions`,
      * a version conflict resolution is delegated to npm/yarn. This behavior may reduce a need to configure
      * `npmResolutions` explicitly. E.g. "14.4.2" can be automatically-picked for ">=14.0.0 14.4.2 ^14.4.1".
      *
      * Note that this key must be scoped by a `Configuration` (either `Compile` or `Test`).
      *
      * @group settings
      */
    val npmResolutions: SettingKey[Map[String, String]] =
      settingKey[Map[String, String]]("NPM dependencies resolutions in case of conflict")

    /**
      * List of the additional configuration options to include in the generated 'package.json'.
      * Note that package dependencies are automatically generated from `npmDependencies` and
      * `npmDevDependencies` and should '''not''' be specified in this setting.
      *
      * {{{
      *   import scalajsbundler.util.JSON._
      *   additionalNpmConfig in Compile := Map(
      *     "name"        -> str(name.value),
      *     "version"     -> str(version.value),
      *     "description" -> str("Awesome ScalaJS project..."),
      *     "other"       -> obj(
      *       "value0" -> bool(true),
      *       "value1" -> obj(
      *         "foo" -> str("bar")
      *       )
      *     )
      *   )
      * }}}
      *
      * Note that this key must be scoped by a `Configuration` (either `Compile` or `Test`).
      *
      * @group settings
      */
    val additionalNpmConfig: SettingKey[Map[String, JSON]] =
      settingKey[Map[String, JSON]]("Additional option to include in the generated 'package.json'")

    /**
      * [[scalajsbundler.BundlingMode]] to use.
      *
      * Must be one of:
      *   `Application`             - Process the entire Scala.js output file with webpack, producing a bundle including all dependencies
      *   `LibraryOnly()`           - Process only the entrypoints via webpack and produce a library of dependencies
      *   `LibraryAndApplication()  - Process only the entrypoints, concatenating the library with the application to produce a bundle
      *
      * The default value is `Application`
      */
    val webpackBundlingMode: SettingKey[BundlingMode] =
      settingKey[BundlingMode]("Bundling mode, one of BundlingMode.{ Application,  LibraryOnly, LibraryAndApplication }.")

    /**
      * Bundles the output of a Scala.js stage.
      *
      * This task must be scoped by a Scala.js stage (either `fastOptJS` or `fullOptJS`) and
      * a `Configuration` (either `Compile` or `Test`).
      *
      * For instance, to bundle the output of `fastOptJS`, run the following task from the sbt shell:
      *
      * {{{
      *   fastOptJS::webpack
      * }}}
      *
      * Or, in an sbt build definition:
      *
      * {{{
      *   webpack in (Compile, fastOptJS)
      * }}}
      *
      * Note that to scope the task to a different project than the “current” sbt project, you
      * have to write the following:
      *
      * {{{
      *   webpack in (projectRef, Compile, fastOptJS in projectRef)
      * }}}
      *
      * The task returns the produced bundles.
      *
      * The task is cached, so webpack is only launched when some of the
      * used files have changed. The list of files to be monitored is
      * provided by webpackMonitoredFiles
      *
      * The files produced are wrapped in [[sbt.Attributed]], and tagged with
      * [[scalajsbundler.sbtplugin.SBTBundlerFile.ProjectNameAttr]] and
      * [[scalajsbundler.sbtplugin.SBTBundlerFile.BundlerFileTypeAttr]]. The
      * [[scalajsbundler.sbtplugin.SBTBundlerFile.ProjectNameAttr]] contains the "prefix" of the file names, such
      * as `yourapp-fastopt`, while the
      * [[scalajsbundler.sbtplugin.SBTBundlerFile.BundlerFileTypeAttr]] contains the bundle file type, which can be
      * used to filter the list of files by their [[scalajsbundler.BundlerFileType]]. For example:
      *
      * {{{
      *   webpack.value.find(_.metadata.get(BundlerFileTypeAttr).exists(_ == BundlerFileType.ApplicationBundle))
      * }}}
      *
      * @group tasks
      */
    val webpack: TaskKey[Seq[Attributed[File]]] =
      taskKey[Seq[Attributed[File]]]("Bundle the output of a Scala.js stage using webpack")

    /**
      * configuration file to use with webpack. By default, the plugin generates a
      * configuration file, but you can supply your own file via this setting. Example of use:
      *
      * {{{
      *   webpackConfigFile in fullOptJS := Some(baseDirectory.value / "my.dev.webpack.config.js")
      * }}}
      *
      * You can find more insights on how to write a custom configuration file in the
      * [[http://scalacenter.github.io/scalajs-bundler/cookbook.html#custom-config cookbook]].
      *
      * @group settings
      */
    val webpackConfigFile: SettingKey[Option[File]] =
      settingKey[Option[File]]("Configuration file to use with webpack")

    /**
      * Webpack configuration files to copy to the target directory. These files can be merged into the main
      * configuration file.
      *
      * By default all .js files in the project base directory are copied:
      *
      * {{{
      *   baseDirectory.value * "*.js"
      * }}}
      *
      * How to share these configuration files among your webpack config files is documented in the
      * [[http://scalacenter.github.io/scalajs-bundler/cookbook.html#shared-config cookbook]].
      *
      * @group settings
      */
    val webpackResources: SettingKey[PathFinder] =
      settingKey[PathFinder]("Webpack resources to copy to target directory (defaults to *.js)")

    /**
      * whether to enable (or not) source-map in
      * a given configuration (`Compile` or `Test`) and stage (`fastOptJS` or `fullOptJS`). Example
      * of use:
      *
      * {{{
      *   webpackEmitSourceMaps in (Compile, fullOptJS) := false
      * }}}
      *
      * Note that, by default, this setting takes the same value as the Scala.js’ `emitSourceMaps`
      * setting, so, to globally disable source maps you can just configure the `emitSourceMaps`
      * setting:
      *
      * {{{
      *   emitSourceMaps := false
      * }}}
      *
      * @group settings
      */
    val webpackEmitSourceMaps: SettingKey[Boolean] =
      settingKey[Boolean]("Whether webpack should emit source maps at all")

    /**
      * Additional directories, monitored for webpack launch.
      *
      * Changes to files in these directories that match
      * `includeFilter` scoped to `webpackMonitoredFiles` enable
      *  webpack launch in `webpack` task.
      *
      * Defaults to an empty `Seq`.
      *
      * @group settings
      * @see [[webpackMonitoredFiles]]
      */
    val webpackMonitoredDirectories: SettingKey[Seq[File]] =
      settingKey[Seq[File]]("Directories, monitored for webpack launch")

    /**
      * List of files, monitored for webpack launch.
      *
      * By default includes the following files:
      *  - Generated `package.json`
      *  - Generated webpack config
      *  - Custom webpack config (if any)
      *  - Files, provided by `webpackEntries` task.
      *  - Files from `webpackMonitoredDirectories`, filtered by
      *    `includeFilter`
      *
      * @group settings
      * @see [[webpackMonitoredDirectories]]
      * @see [[webpack]]
      */
    val webpackMonitoredFiles: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Files that trigger webpack launch")

    /**
      * Whether to use [[https://yarnpkg.com/ Yarn]] to fetch dependencies instead
      * of `npm`. Yarn has a caching mechanism that makes the process faster.
      *
      * If set to `true`, it requires Yarn 0.22.0+ to be available on the
      * host platform.
      *
      * Defaults to `false`.
      *
      * @group settings
      */
    val useYarn: SettingKey[Boolean] =
      settingKey[Boolean]("Whether to use yarn for updates")

    /**
      * Port, on which webpack-dev-server will be launched.
      *
      * Defaults to 8080.
      *
      * @see [[startWebpackDevServer]]
      * @group settings
      */
    val webpackDevServerPort = SettingKey[Int](
      "webpackDevServerPort",
      "Port, on which webpack-dev-server operates"
    )

    /**
      * Additional arguments to webpack-dev-server.
      *
      * Defaults to an empty list.
      *
      * @see [[startWebpackDevServer]]
      * @group settings
      */
    val webpackDevServerExtraArgs = SettingKey[Seq[String]](
      "webpackDevServerExtraArgs",
      "Custom arguments to webpack-dev-server"
    )

    /**
      * Start background webpack-dev-server process.
      *
      * If webpack-dev-server is already running, it will be restarted.
      *
      * The started webpack-dev-server receives the following arguments:
      * - `--config` is set to value of `webpackConfigFile` setting.
      * - `--port` is set to value of `webpackDevServerPort` setting.
      * - Contents of `webpackDevServerExtraArgs` setting.
      *
      * @see [[stopWebpackDevServer]]
      * @see [[webpackDevServerPort]]
      * @see [[webpackDevServerExtraArgs]]
      * @group tasks
      */
    val startWebpackDevServer = TaskKey[Unit](
      "startWebpackDevServer",
      "(Re)start webpack-dev-server process"
    )

    /**
      * Stop running webpack-dev-server process.
      *
      * Does nothing if the server is not running.
      *
      * @see [[startWebpackDevServer]]
      * @group tasks
      */
    val stopWebpackDevServer = TaskKey[Unit](
      "stopWebpackDevServer",
      "Stop webpack-dev-server process (if running)"
    )

    /**
      * Locally install jsdom.
      *
      * You can set the jsdom package version to install with the key `version in installJsdom`.
      *
      * Returns the installation directory.
      *
      * @group tasks
      */
    val installJsdom = taskKey[File]("Locally install jsdom")
  }

  private val scalaJSBundlerPackageJson =
    TaskKey[BundlerFile.PackageJson]("scalaJSBundlerPackageJson",
      "Write a package.json file defining the NPM dependencies of project",
      KeyRanks.Invisible
    )

  private[sbtplugin] val scalaJSBundlerWebpackConfig =
    TaskKey[BundlerFile.WebpackConfig]("scalaJSBundlerWebpackConfig",
      "Write the webpack configuration file",
      KeyRanks.Invisible
    )

  private val webpackDevServer = SettingKey[WebpackDevServer](
    "webpackDevServer",
    "Global WebpackDevServer instance",
    KeyRanks.Invisible
  )

  private[scalajsbundler] val ensureModuleKindIsCommonJSModule =
    SettingKey[Boolean](
      "ensureModuleKindIsCommonJSModule",
      "Checks that scalaJSModuleKind is set to CommonJSModule",
      KeyRanks.Invisible
    )

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(

    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },

    version in webpack := "3.5.5",

    version in startWebpackDevServer := "2.7.1",

    version in installJsdom := "9.9.0",

    webpackConfigFile := None,

    webpackResources := baseDirectory.value * "*.js",

    // Include the manifest in the produced artifact
    (products in Compile) := (products in Compile).dependsOn(scalaJSBundlerManifest).value,

    useYarn := false,

    ensureModuleKindIsCommonJSModule := {
      if (scalaJSModuleKind.value == ModuleKind.CommonJSModule) true
      else sys.error(s"scalaJSModuleKind must be set to ModuleKind.CommonJSModule in projects where ScalaJSBundler plugin is enabled")
    },

    webpackBundlingMode := BundlingMode.Default,

    // Make these settings project-level, since we don't expect much
    // difference between configurations/stages. This way the
    // API user can modify it just once.
    webpackMonitoredDirectories := Seq(),
    (includeFilter in webpackMonitoredFiles) := AllPassFilter,

    // The defaults are specified at top level.
    webpackDevServerPort := 8080,
    webpackDevServerExtraArgs := Seq(),

    // We can only have one server per project - for simplicity
    webpackDevServer := new WebpackDevServer(),

    (onLoad in Global) := {
      (onLoad in Global).value.compose(
        _.addExitHook {
          webpackDevServer.value.stop()
        }
      )
    },

    installJsdom := {
      val installDir = target.value / "scalajs-bundler-jsdom"
      val log = streams.value.log
      val jsdomVersion = (version in installJsdom).value
      if (!installDir.exists()) {
        log.info(s"Installing jsdom in ${installDir.absolutePath}")
        IO.createDirectory(installDir)
        install(installDir, useYarn.value, log)(s"jsdom@$jsdomVersion")
      }
      installDir
    }
  ) ++
    inConfig(Compile)(perConfigSettings) ++
    inConfig(Test)(perConfigSettings ++ testSettings)

  private lazy val perConfigSettings: Seq[Def.Setting[_]] =
    Seq(
      npmDependencies := Seq.empty,

      npmDevDependencies := Seq.empty,

      npmResolutions := Map.empty,

      additionalNpmConfig := Map(
        "private" -> JSON.bool(true),
        "license" -> JSON.str("UNLICENSED")
      ),

      npmUpdate := {
        val log = streams.value.log
        val targetDir = (crossTarget in npmUpdate).value
        val jsResources = scalaJSNativeLibraries.value.data
        val packageJsonFile = scalaJSBundlerPackageJson.value

        val cachedActionFunction =
          FileFunction.cached(
            streams.value.cacheDirectory / "scalajsbundler-npm-update",
            inStyle = FilesInfo.hash
          ) { _ =>
            log.info("Updating NPM dependencies")
            if (useYarn.value) {
              Yarn.run("install", "--non-interactive")(targetDir, log)
            } else {
              Npm.run("install")(targetDir, log)
            }
            jsResources.foreach { resource =>
              IO.write(targetDir / resource.relativePath, resource.content)
            }
            Set.empty
          }

        cachedActionFunction(Set(packageJsonFile.file) ++
          jsResources.collect { case f: FileVirtualJSFile => f.file }.to[Set])

        targetDir
      },

      scalaJSBundlerPackageJson :=
        PackageJsonTasks.writePackageJson(
          (crossTarget in npmUpdate).value,
          npmDependencies.value,
          npmDevDependencies.value,
          npmResolutions.value,
          additionalNpmConfig.value,
          fullClasspath.value,
          configuration.value,
          (version in webpack).value,
          (version in startWebpackDevServer).value,
          streams.value
        ),

      crossTarget in npmUpdate := {
        crossTarget.value / "scalajs-bundler" / (if (configuration.value == Compile) "main" else "test")
      },

      // Override Scala.js’ loadedJSEnv to first run `npm update`
      loadedJSEnv := loadedJSEnv.dependsOn(npmUpdate).value
    ) ++
    perScalaJSStageSettings(Stage.FastOpt) ++
    perScalaJSStageSettings(Stage.FullOpt)

  private val createdTestAdapters = new AtomicReference[List[TestAdapter]](Nil)

  @tailrec
  private def newTestAdapter(jsEnv: ComJSEnv, config: TestAdapter.Config): TestAdapter = {
    val prev = createdTestAdapters.get()
    val r = new TestAdapter(jsEnv, config)
    if (createdTestAdapters.compareAndSet(prev, r :: prev)) r
    else newTestAdapter(jsEnv, config)
  }

  private def closeAllTestAdapters(): Unit =
    createdTestAdapters.getAndSet(Nil).foreach(_.close())

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    onComplete := {
      val prev = onComplete.value

      { () =>
        prev()
        closeAllTestAdapters()
      }
    }
  )

  private lazy val testSettings: Seq[Setting[_]] =
    Seq(
      npmDependencies ++= (npmDependencies in Compile).value,

      npmDevDependencies ++= (npmDevDependencies in Compile).value,

      // Override Scala.js setting, which does not support the combination of jsdom and CommonJS module output kind
      loadedTestFrameworks := Def.task {
        // use assert to prevent warning about pure expr in stat pos
        assert(scalaJSEnsureUnforked.value)

        val console = scalaJSConsole.value
        val toolsLogger = sbtLogger2ToolsLogger(streams.value.log)
        val frameworks = testFrameworks.value
        val sjsOutput = fastOptJS.value.data

        val env =
          jsEnv.?.value.map {
            case comJSEnv: ComJSEnv => comJSEnv.loadLibs(Seq(ResolvedJSDependency.minimal(FileVirtualJSFile(sjsOutput))))
            case other => sys.error(s"You need a ComJSEnv to test (found ${other.name})")
          }.getOrElse {
            Def.taskDyn[ComJSEnv] {
              assert(ensureModuleKindIsCommonJSModule.value)
              val sjsOutput = fastOptJS.value.data
              // If jsdom is going to be used, then we should bundle the test module into a file that exports the tests to the global namespace
              if ((scalaJSRequestsDOM in fastOptJS).value) Def.task {
                val logger = streams.value.log
                val targetDir = npmUpdate.value
                val sjsOutputName = sjsOutput.name.stripSuffix(".js")
                val bundle = targetDir / s"$sjsOutputName-bundle.js"

                val customWebpackConfigFile = (webpackConfigFile in Test).value

                val writeTestBundleFunction =
                  FileFunction.cached(
                    streams.value.cacheDirectory / "test-loader",
                    inStyle = FilesInfo.hash
                  ) { _ =>
                    logger.info("Writing and bundling the test loader")
                    val loader = targetDir / s"$sjsOutputName-loader.js"
                    JsDomTestEntries.writeLoader(sjsOutput, loader)

                    customWebpackConfigFile match {
                      case Some(configFile) =>
                        val customConfigFileCopy = Webpack.copyCustomWebpackConfigFiles(targetDir, webpackResources.value.get)(configFile)
                        Webpack.run("--config", customConfigFileCopy.getAbsolutePath, loader.absolutePath, bundle.absolutePath)(targetDir, logger)
                      case None =>
                        Webpack.run(loader.absolutePath, bundle.absolutePath)(targetDir, logger)
                    }

                    Set.empty
                  }
                writeTestBundleFunction(Set(sjsOutput))
                val file = FileVirtualJSFile(bundle)

                val jsdomDir = installJsdom.value
                new JSDOMNodeJSEnv(jsdomDir).loadLibs(Seq(ResolvedJSDependency.minimal(file)))
              } else Def.task {
                new NodeJSEnv().loadLibs(Seq(ResolvedJSDependency.minimal(FileVirtualJSFile(sjsOutput))))
              }
            }.value
          }

        // Pretend that we are not using a CommonJS module if jsdom is involved, otherwise that
        // would be incompatible with the way jsdom loads scripts
        val (moduleKind, moduleIdentifier) = {
          val withoutDom = (scalaJSModuleKind.value, scalaJSModuleIdentifier.value)

          if ((scalaJSRequestsDOM in fastOptJS).value) (ModuleKind.NoModule, None)
          else withoutDom
        }

        val frameworkNames = frameworks.map(_.implClassNames.toList).toList

        val config = TestAdapter.Config()
          .withLogger(toolsLogger)
          .withJSConsole(console)
          .withModuleSettings(moduleKind, moduleIdentifier)

        val adapter = new TestAdapter(env, config)
        val frameworkAdapters = adapter.loadFrameworks(frameworkNames)

        frameworks.zip(frameworkAdapters).collect {
          case (tf, Some(adapter)) => (tf, adapter)
        }.toMap
      }.dependsOn(npmUpdate).value
    )

  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      // Ask Scala.js to output its result in our target directory
      crossTarget in stageTask := (crossTarget in npmUpdate).value,

      // Override Scala.js’ relativeSourceMaps in case we have to emit source maps in the webpack task, because it does not work with absolute source maps
      relativeSourceMaps in stageTask := (webpackEmitSourceMaps in stageTask).value,

      scalaJSBundlerWebpackConfig in stageTask := BundlerFile.WebpackConfig(
        WebpackTasks.entry(stageTask).value,
        npmUpdate.value / "scalajs.webpack.config.js"
      ),

      webpack in stageTask := Def.taskDyn {
        (webpackBundlingMode in stageTask).value match {
          case scalajsbundler.BundlingMode.Application =>
            WebpackTasks.webpack(stageTask)
          case mode: scalajsbundler.BundlingMode.LibraryOnly =>
            LibraryTasks.librariesAndLoaders(stageTask, mode)
          case mode: scalajsbundler.BundlingMode.LibraryAndApplication =>
            LibraryTasks.libraryAndLoadersBundle(stageTask, mode)
        }
      }.value,

      webpackEmitSourceMaps in stageTask := (emitSourceMaps in stageTask).value,

      webpackMonitoredFiles in stageTask := {
        val generatedWebpackConfigFile = (scalaJSBundlerWebpackConfig in stageTask).value
        val customWebpackConfigFile = (webpackConfigFile in stageTask).value
        val packageJsonFile = scalaJSBundlerPackageJson.value
        val entry = WebpackTasks.entry(stageTask).value
        val filter = (includeFilter in webpackMonitoredFiles).value
        val dirs = (webpackMonitoredDirectories in stageTask).value

        val generatedFiles: Seq[File] = Seq(
          packageJsonFile.file,
          generatedWebpackConfigFile.file ,
          entry.file
        )
        val additionalFiles: Seq[File] = dirs.flatMap(
          dir => (dir ** filter).get
        )
        generatedFiles ++
          customWebpackConfigFile.toSeq ++
          webpackResources.value.get ++
          additionalFiles
      },

      // webpack-dev-server wiring
      startWebpackDevServer in stageTask := Def.task {
        val port = (webpackDevServerPort in stageTask).value
        val extraArgs = (webpackDevServerExtraArgs in stageTask).value

        // This duplicates file layout logic from `Webpack`
        val targetDir = (npmUpdate in stageTask).value
        val customConfigOption = (webpackConfigFile in stageTask).value
        val generatedConfig = (scalaJSBundlerWebpackConfig in stageTask).value

        val config = customConfigOption
          .map(Webpack.copyCustomWebpackConfigFiles(targetDir, webpackResources.value.get))
          .getOrElse(generatedConfig.file)

        // To match `webpack` task behavior
        val workDir = targetDir

        // Server instance is project-level
        val server = webpackDevServer.value
        val logger = (streams in stageTask).value.log

        server.start(
          workDir,
          config,
          port,
          extraArgs,
          logger
        )
      }.dependsOn(
        // We need to execute the full webpack task once, since it generates
        // the required config file
        (webpack in stageTask),

        npmUpdate
      ).value,

      // Stops the global server instance, but is defined on stage
      // level to match `startWebpackDevServer`
      stopWebpackDevServer in stageTask := {
        webpackDevServer.value.stop()
      }
    )
  }

  /**
    * Writes the scalajs-bundler manifest file.
    */
  val scalaJSBundlerManifest: Def.Initialize[Task[File]] =
    Def.task {
      NpmDependencies.writeManifest(
        NpmDependencies(
          (npmDependencies in Compile).value.to[List],
          (npmDependencies in Test).value.to[List],
          (npmDevDependencies in Compile).value.to[List],
          (npmDevDependencies in Test).value.to[List]
        ),
        (classDirectory in Compile).value
      )
    }

}
