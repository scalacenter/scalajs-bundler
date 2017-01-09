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
      * @group tasks
      */
    val webpack: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Bundle the output of a Scala.js stage using webpack")

    /**
      * configuration file to use with webpack. By default, the plugin generates a
      * configuration file, but you can supply your own file via this setting. Example of use:
      *
      * {{{
      *   webpackConfigFile in fullOptJS := Some(baseDirectory.value / "my.prod.webpack.config.js")
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
      * whether to enable the “reload workflow” for `webpack in fastOptJS`.
      *
      * When enabled, dependencies are pre-bundled so that the output of `fastOptJS` can directly
      * be executed by a web browser without being further processed by a bundling system. This
      * reduces the delays when live-reloading the application on source modifications. Defaults
      * to `false`.
      *
      * Note that the “reload workflow” does '''not''' use the custom webpack configuration file,
      * if any.
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

  }

  private val scalaJSBundlerPackageJson =
    TaskKey[File]("scalaJSBundlerPackageJson", "Write a package.json file defining the NPM dependencies of project", KeyRanks.Invisible)

  private val scalaJSBundlerWebpackConfig =
    TaskKey[File]("scalaJSBundlerWebpackConfig", "Write the webpack configuration file", KeyRanks.Invisible)

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

    webpackConfigFile := None,

    // Include the manifest in the produced artifact
    (products in Compile) := (products in Compile).dependsOn(scalaJSBundlerManifest).value,

    enableReloadWorkflow := false,

    useYarn := false,

    ensureModuleKindIsCommonJSModule := {
      if (scalaJSModuleKind.value == ModuleKind.CommonJSModule) true
      else sys.error(s"scalaJSModuleKind must be set to ModuleKind.CommonJSModule in projects where ScalaJSBundler plugin is enabled")
    }

  ) ++
    inConfig(Compile)(perConfigSettings) ++
    inConfig(Test)(perConfigSettings ++ testSettings)

  private lazy val perConfigSettings: Seq[Def.Setting[_]] =
    Seq(
      npmDependencies := Seq.empty,

      npmDevDependencies := Seq.empty,

      npmResolutions := Map.empty,

      webpack in fullOptJS := webpackTask(fullOptJS).value,

      webpack in fastOptJS := Def.taskDyn {
        if (enableReloadWorkflow.value) ReloadWorkflowTasks.webpackTask(configuration.value, fastOptJS)
        else webpackTask(fastOptJS)
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

                val writeTestBundleFunction =
                  FileFunction.cached(
                    streams.value.cacheDirectory / "test-loader",
                    inStyle = FilesInfo.hash
                  ) { _ =>
                    logger.info("Writing and bundling the test loader")
                    val loader = targetDir / s"$sjsOutputName-loader.js"
                    JsDomTestEntries.writeLoader(sjsOutput, loader)
                    Webpack.run(loader.absolutePath, bundle.absolutePath)(targetDir, logger)
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

      webpackEmitSourceMaps in stageTask := (emitSourceMaps in stageTask).value

    )
  }

  def webpackTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      assert(ensureModuleKindIsCommonJSModule.value)
      val log = streams.value.log
      val targetDir = npmUpdate.value
      val generatedWebpackConfigFile = (scalaJSBundlerWebpackConfig in stage).value
      val customWebpackConfigFile = (webpackConfigFile in stage).value
      val packageJsonFile = scalaJSBundlerPackageJson.value
      val entries = (webpackEntries in stage).value

      val cachedActionFunction =
        FileFunction.cached(
          streams.value.cacheDirectory / s"${stage.key.label}-webpack",
          inStyle = FilesInfo.hash
        ) { _ =>
          Webpack.bundle(
            generatedWebpackConfigFile,
            customWebpackConfigFile,
            entries,
            targetDir,
            log
          ).to[Set]
        }
      cachedActionFunction(Set(
        generatedWebpackConfigFile,
        packageJsonFile
      ) ++
        (webpackConfigFile in stage).value.map(Set(_)).getOrElse(Set.empty) ++
        entries.map(_._2).to[Set] + stage.value.data).to[Seq] // Note: the entries should be enough, excepted that they currently are launchers, which do not change even if the scalajs stage output changes
    }

  /**
    * Locally installs jsdom.
    *
    * @return Installation directory
    */
  lazy val installJsdom: Def.Initialize[Task[File]] =
    Def.task {
      val installDir = target.value / "scalajs-bundler-jsdom"
      val log = streams.value.log
      if (!installDir.exists()) {
        log.info(s"Installing jsdom in ${installDir.absolutePath}")
        IO.createDirectory(installDir)
        Npm.run("install", "jsdom")(installDir, log)
      }
      installDir
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
