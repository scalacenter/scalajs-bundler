package scalajsbundler.sbtplugin

import org.scalajs.core.tools.io.{FileVirtualJSFile, VirtualJSFile}
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.core.tools.linker.backend.ModuleKind
import org.scalajs.jsenv.ComJSEnv
import org.scalajs.sbtplugin.Loggers.sbtLogger2ToolsLogger
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPluginInternal.{scalaJSEnsureUnforked, scalaJSModuleIdentifier, scalaJSRequestsDOM}
import org.scalajs.sbtplugin.{FrameworkDetectorWrapper, ScalaJSPlugin, ScalaJSPluginInternal, Stage}
import org.scalajs.testadapter.ScalaJSFramework
import sbt.Keys._
import sbt._

import scalajsbundler.ExternalCommand.install
import scalajsbundler._

/**
  * This plugin enables `ScalaJSPlugin` and sets the `scalaJSModuleKind` to `CommonJSModule`. It also makes it
  * possible to define dependencies to NPM packages and provides tasks to fetch them or to bundle the application
  * with its dependencies.
  *
  * = Tasks and Settings =
  *
  * The [[ScalaJSBundlerPlugin$.autoImport autoImport]] member documents the keys provided by this plugin. Besides these keys, the
  * following existing keys also control the plugin:
  *
  * == `version in webpack` ==
  *
  * Version of webpack to use. Example:
  *
  * {{{
  *   version in webpack := "2.1.0-beta.25"
  * }}}
  *
  * == `version in installJsdom` ==
  *
  * Version of jsdom to use.
  *
  * == `version in installWebpackDevServer` ==
  *
  * Version of webpack-dev-server to use.
  *
  * == `crossTarget in npmUpdate` ==
  *
  * The directory in which NPM dependencies will be fetched, and where all the .js files
  * will be generated. The directory is different according to the current `Configuration`
  * (either `Compile` or `Test`).
  *
  * Defaults to `crossTarget.value / "scalajs-bundler" / "main"` for `Compile` and
  * `crossTarget.value / "scalajs-bundler" / "test"` for `Test`.
  *
  * == `scalaJSLauncher` ==
  *
  * The launcher for Scala.js’ stage output (e.g. `scalaJSLauncher in fastOptJS`).
  * The launcher runs the “main” of the application.
  */
object ScalaJSBundlerPlugin extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  // Exported keys
  /**
    * @groupname tasks Tasks
    * @groupname settings Settings
    */
  object autoImport {

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
      * Note that this key must be scoped by a `Configuration` (either `Compile` or `Test`).
      *
      * @group settings
      */
    val npmResolutions: SettingKey[Map[String, String]] =
      settingKey[Map[String, String]]("NPM dependencies resolutions in case of conflict")

    /**
      * List of the additional package options to include in the generated 'package.json'.
      *
      * {{{
      *   import scalajsbundler.util.JS._
      *   npmConfig in Compile := Map(
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
    val npmConfig: SettingKey[Map[String, util.JS]] =
      settingKey[Map[String, util.JS]]("Additional option to include in the generated 'package.json'")

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
      * @group tasks
      */
    val webpack: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Bundle the output of a Scala.js stage using webpack")

    /**
      * Bundles the output of a Scala.js stage using the reload workflow.
      *
      * This is equivalent to running fastOptJS::webpack with reloadWorkflow := true.
      * This task must be scoped by the Scala.js fastOptJS stage.
      *
      * For instance, to bundle the output of `fastOptJS`, run the following task from the sbt shell:
      *
      * {{{
      *   fastOptJS::webpackReload
      * }}}
      *
      * To use a custom webpack configuration file use:
      *
      * {{{
      *   webpackConfigFile in webpackReload := Some(baseDirectory.value / "webpack-reload.config.js"),
      * }}}
      *
      * @group tasks
      */
    val webpackReload: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Bundle the output of a Scala.js stage by appending the generated javascript to the pre-bundled dependencies")


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
      * List of entry bundles to generate. By default it generates just one bundle
      * for your main class.
      *
      * @group tasks
      */
    val webpackEntries: TaskKey[Seq[(String, File)]] =
      taskKey[Seq[(String, File)]]("Webpack entry bundles")

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
      * whether to enable the “reload workflow” for `webpack in fastOptJS`.
      *
      * When enabled, dependencies are pre-bundled so that the output of `fastOptJS` can directly
      * be executed by a web browser without being further processed by a bundling system. This
      * reduces the delays when live-reloading the application on source modifications. Defaults
      * to `false`.
      *
      * Note that the “reload workflow” does uses the custom webpack configuration file scoped to
      * the webpackReload task.
      *
      * For example:
      *
      * {{{
      *   webpackConfigFile in webpackReload := Some(baseDirectory.value / "webpack-reload.config.js"),
      * }}}
      *
      * @group settings
      */
    val enableReloadWorkflow: SettingKey[Boolean] =
      settingKey[Boolean]("Whether to enable the reload workflow for fastOptJS")

    /**
      * Whether to use [[https://yarnpkg.com/ Yarn]] to fetch dependencies instead
      * of `npm`. Yarn has a caching mechanism that makes the process faster.
      *
      * If set to `true`, it requires the `yarn` command to be available in the
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

    /**
      * Locally install webpack-dev-server.
      *
      * You can set the webpack-dev-server package version to install with the key `version in installWebpackDevServer`.
      *
      * Returns the installation directory.
      *
      * @group tasks
      */
    val installWebpackDevServer = taskKey[File]("Locally install webpack-dev-server")

  }

  private val scalaJSBundlerPackageJson =
    TaskKey[File]("scalaJSBundlerPackageJson", "Write a package.json file defining the NPM dependencies of project", KeyRanks.Invisible)

  private val scalaJSBundlerWebpackConfig =
    TaskKey[File]("scalaJSBundlerWebpackConfig", "Write the webpack configuration file", KeyRanks.Invisible)

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

    scalaJSModuleKind := ModuleKind.CommonJSModule,

    version in webpack := "1.14",

    version in installWebpackDevServer := "1.16.3",

    version in installJsdom := "9.9.0",

    webpackConfigFile := None,

    webpackResources := baseDirectory.value * "*.js",

    // Include the manifest in the produced artifact
    (products in Compile) := (products in Compile).dependsOn(scalaJSBundlerManifest).value,

    enableReloadWorkflow := false,

    useYarn := false,

    ensureModuleKindIsCommonJSModule := {
      if (scalaJSModuleKind.value == ModuleKind.CommonJSModule) true
      else sys.error(s"scalaJSModuleKind must be set to ModuleKind.CommonJSModule in projects where ScalaJSBundler plugin is enabled")
    },

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
    },

    installWebpackDevServer := {
      val installDir = target.value / "scalajs-bundler-webpack-dev-server"
      val log = streams.value.log
      val webpackVersion = (version in webpack).value
      val webpackDevServerVersion = (version in installWebpackDevServer).value

      if (!installDir.exists()) {
        log.info(s"Installing webpack-dev-server in ${installDir.absolutePath}")
        IO.createDirectory(installDir)
        install(installDir, useYarn.value, log)(
          // Webpack version should match the setting
          s"webpack@$webpackVersion",
          s"webpack-dev-server@$webpackDevServerVersion"
        )
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

      npmConfig := Map.empty,

      webpack in fullOptJS := webpackTask(fullOptJS).value,

      webpack in fastOptJS := Def.taskDyn {
        if (enableReloadWorkflow.value) ReloadWorkflowTasks.webpackTask(configuration.value, fastOptJS)
        else webpackTask(fastOptJS)
      }.value,

      webpackReload in fastOptJS := Def.taskDyn {
        ReloadWorkflowTasks.webpackTask(configuration.value, fastOptJS)
      }.value,

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
              Yarn.run("install")(targetDir, log)
            } else {
              Npm.run("update")(targetDir, log)
            }
            jsResources.foreach { resource =>
              IO.write(targetDir / resource.relativePath, resource.content)
            }
            Set.empty
          }

        cachedActionFunction(Set(packageJsonFile) ++ jsResources.collect { case f: FileVirtualJSFile => f.file }.to[Set])

        targetDir
      },

      scalaJSBundlerPackageJson :=
        PackageJsonTasks.writePackageJson(
          (crossTarget in npmUpdate).value,
          npmDependencies.value,
          npmDevDependencies.value,
          npmResolutions.value,
          npmConfig.value,
          fullClasspath.value,
          configuration.value,
          (version in webpack).value,
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
                NodeJSEnv().value.loadLibs(Seq(ResolvedJSDependency.minimal(FileVirtualJSFile(sjsOutput))))
              }
            }.value
          }

        // Pretend that we are not using a CommonJS module if jsdom is involved, otherwise that
        // would be incompatible with the way jsdom loads scripts
        val (moduleKind, moduleIdentifier) =
          if ((scalaJSRequestsDOM in fastOptJS).value) (ModuleKind.NoModule, None)
          else (scalaJSModuleKind.value, scalaJSModuleIdentifier.value)

        val detector =
          new FrameworkDetectorWrapper(env, moduleKind, moduleIdentifier).wrapped

        detector.detect(frameworks, toolsLogger).map { case (tf, frameworkName) =>
          val framework =
            new ScalaJSFramework(frameworkName, env, moduleKind, moduleIdentifier, toolsLogger, console)
          (tf, framework)
        }
      }.dependsOn(npmUpdate).value
    )

  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      // Ask Scala.js to output its result in our target directory
      crossTarget in stageTask := (crossTarget in npmUpdate).value,

      // Override Scala.js’ scalaJSLauncher to add support for CommonJSModule
      scalaJSLauncher in stageTask := {
        val launcher =
          Launcher.write(
            (crossTarget in npmUpdate).value,
            stageTask.value,
            stage,
            (mainClass in (scalaJSLauncher in stageTask)).value.getOrElse(sys.error("No main class detected"))
          )
        Attributed[VirtualJSFile](FileVirtualJSFile(launcher.file))(
          AttributeMap.empty.put(name.key, launcher.mainClass)
        )
      },

      // Override Scala.js’ relativeSourceMaps in case we have to emit source maps in the webpack task, because it does not work with absolute source maps
      relativeSourceMaps in stageTask := (webpackEmitSourceMaps in stageTask).value,

      webpackEntries in stageTask := {
        val launcherFile =
          (scalaJSLauncher in stageTask).value.data match {
            case f: FileVirtualJSFile => f.file
            case _ => sys.error("Unable to find the launcher (real) file")
          }
        val stageFile = stageTask.value.data
        val name = stageFile.name.stripSuffix(".js")
        Seq(name -> launcherFile)
      },

      scalaJSBundlerWebpackConfig in stageTask :=
        Webpack.writeConfigFile(
          (webpackEmitSourceMaps in stageTask).value,
          (webpackEntries in stageTask).value,
          npmUpdate.value,
          streams.value.log
        ),

      webpackEmitSourceMaps in stageTask := (emitSourceMaps in stageTask).value,

      webpackMonitoredFiles in stageTask := {
        val generatedWebpackConfigFile = (scalaJSBundlerWebpackConfig in stageTask).value
        val customWebpackConfigFile = (webpackConfigFile in stageTask).value
        val packageJsonFile = scalaJSBundlerPackageJson.value
        val entries = (webpackEntries in stageTask).value

        val filter = (includeFilter in webpackMonitoredFiles).value
        val dirs = (webpackMonitoredDirectories in stageTask).value

        val additionalFiles = dirs.flatMap(
          dir => (dir ** filter).get
        )

        packageJsonFile +:
          generatedWebpackConfigFile +:
          customWebpackConfigFile ++:
          entries.map(_._2) ++:
          // Entries only contain launchers - we need to monitor
          // Scala.js bundles themselves, too.
          stageTask.value.data +:
          additionalFiles
      },

      // webpack-dev-server wiring
      startWebpackDevServer in stageTask := {
        val serverDir = installWebpackDevServer.value

        // We need to execute the full webpack task once, since it generates
        // the required config file
        (webpack in stageTask).value

        val port = (webpackDevServerPort in stageTask).value
        val extraArgs = (webpackDevServerExtraArgs in stageTask).value

        // This duplicates file layout logic from `Webpack`
        val targetDir = (npmUpdate in stageTask).value
        val customConfigOption = (webpackConfigFile in stageTask).value
        val generatedConfig = (scalaJSBundlerWebpackConfig in stageTask).value

        val config = customConfigOption
          .map(Webpack.copyCustomWebpackConfigFiles(targetDir, webpackResources.value.get))
          .getOrElse(generatedConfig)

        // To match `webpack` task behavior
        val workDir = targetDir

        // Server instance is project-level
        val server = webpackDevServer.value
        val logger = (streams in stageTask).value.log

        server.start(
          serverDir,
          workDir,
          config,
          port,
          extraArgs,
          logger
        )
      },

      // Stops the global server instance, but is defined on stage
      // level to match `startWebpackDevServer`
      stopWebpackDevServer in stageTask := {
        webpackDevServer.value.stop()
      }
    )
  }

  def webpackTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val log = streams.value.log
      val targetDir = npmUpdate.value
      val generatedWebpackConfigFile = (scalaJSBundlerWebpackConfig in stage).value
      val customWebpackConfigFile = (webpackConfigFile in stage).value
      val webpackResourceFiles = webpackResources.value.get
      val entries = (webpackEntries in stage).value
      val monitoredFiles = (webpackMonitoredFiles in stage).value

      val cachedActionFunction =
        FileFunction.cached(
          streams.value.cacheDirectory / s"${stage.key.label}-webpack",
          inStyle = FilesInfo.hash
        ) { _ =>
          Webpack.bundle(
            generatedWebpackConfigFile,
            customWebpackConfigFile,
            webpackResourceFiles,
            entries,
            targetDir,
            log
          ).to[Set]
        }

      cachedActionFunction(monitoredFiles.to[Set]).to[Seq]
    }

  /**
    * Writes the scalajs-bundler manifest file.
    */
  lazy val scalaJSBundlerManifest: Def.Initialize[Task[File]] =
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
