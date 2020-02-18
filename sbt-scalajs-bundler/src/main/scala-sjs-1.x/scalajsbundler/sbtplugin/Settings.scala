package scalajsbundler.sbtplugin

import scala.annotation.tailrec

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._
import org.scalajs.jsenv.JSEnv
import org.scalajs.linker.interface.{ModuleKind => _, _}
import org.scalajs.sbtplugin._
import org.scalajs.sbtplugin.Loggers.sbtLogger2ToolsLogger
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbt.testing.Framework
import scalajsbundler.{JSDOMNodeJSEnv, Webpack, JsDomTestEntries, NpmPackage}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.{installJsdom, npmUpdate, requireJsDomEnv, webpackConfigFile, webpackNodeArgs, webpackResources, webpack}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.{createdTestAdapters, ensureModuleKindIsCommonJSModule, scalaJSBundlerImportedModules}
import scalajsbundler.sbtplugin.internal.BuildInfo
import scalajsbundler.scalajs.compat.testing.TestAdapter

private[sbtplugin] object Settings {

  private class BundlerLinkerImpl(base: LinkerImpl.Reflect)
      extends LinkerImpl.Forwarding(base) {

    private val loader = base.loader

    private val clearableLinkerMethod = {
      Class.forName("scalajsbundler.bundlerlinker.BundlerLinkerImpl", true, loader)
        .getMethod("clearableLinker", classOf[StandardConfig], classOf[Path])
    }

    def bundlerLinker(config: StandardConfig, entryPointOutputFile: Path): ClearableLinker = {
      clearableLinkerMethod.invoke(null, config, entryPointOutputFile)
        .asInstanceOf[ClearableLinker]
    }
  }

  // Settings that must be applied in Global
  val globalSettings: Seq[Setting[_]] =
    Def.settings(
      fullClasspath in scalaJSLinkerImpl := {
        val s = streams.value
        val log = s.log
        val retrieveDir = s.cacheDirectory / "scalajs-bundler-linker"
        val lm = (dependencyResolution in scalaJSLinkerImpl).value
        val moduleID = "ch.epfl.scala" % "scalajs-bundler-linker_2.12" % BuildInfo.version
        lm.retrieve(moduleID, scalaModuleInfo = None, retrieveDir, log)
          .fold(w => throw w.resolveException, Attributed.blankSeq(_))
      },

      scalaJSLinkerImpl := {
        val cp = (fullClasspath in scalaJSLinkerImpl).value
        scalaJSLinkerImplBox.value.ensure {
          new BundlerLinkerImpl(LinkerImpl.reflect(Attributed.data(cp)))
        }
      }
    )

  // Settings that must be applied for each stage in each configuration
  private def scalaJSStageSettings(stage: Stage, key: TaskKey[Attributed[File]]): Seq[Setting[_]] = {
    val entryPointOutputFileName =
      s"entrypoints-${stage.toString.toLowerCase}.txt"

    Def.settings(
      scalaJSLinker in key := {
        val config = (scalaJSLinkerConfig in key).value
        val box = (scalaJSLinkerBox in key).value
        val linkerImpl = (scalaJSLinkerImpl in key).value
        val projectID = thisProject.value.id
        val configName = configuration.value.name
        val log = streams.value.log
        val entryPointOutputFile = crossTarget.value / entryPointOutputFileName

        if (config.moduleKind != scalaJSLinkerConfig.value.moduleKind) {
          val keyName = key.key.label
          log.warn(
              s"The module kind in `scalaJSLinkerConfig in ($projectID, " +
              s"$configName, $keyName)` is different than the one `in " +
              s"`($projectID, $configName)`. " +
              "Some things will go wrong.")
        }

        box.ensure {
          linkerImpl.asInstanceOf[BundlerLinkerImpl]
            .bundlerLinker(config, entryPointOutputFile.toPath)
        }
      },

      scalaJSBundlerImportedModules in key := {
        val _ = key.value
        val entryPointOutputFile = crossTarget.value / entryPointOutputFileName
        val lines = Files.readAllLines(entryPointOutputFile.toPath, StandardCharsets.UTF_8)
        lines.asScala.toList
      }
    )
  }

  // Settings that must be applied for each configuration
  val configSettings: Seq[Setting[_]] =
    Def.settings(
      // Override Scala.js’ jsEnvInput to first run `npm update`
      jsEnvInput := jsEnvInput.dependsOn(npmUpdate).value,

      scalaJSStageSettings(FastOptStage, fastOptJS),
      scalaJSStageSettings(FullOptStage, fullOptJS)
    )

  // Settings that must be applied in the Test configuration
  val testConfigSettings: Seq[Setting[_]] =
    Def.settings(
      // Override loadedTestFrameworks from Scala.js, which does not support the combination of jsdom and CommonJS module output kind
      loadedTestFrameworks := Def.task {
        val (env, input) = {
          Def.taskDyn[(JSEnv, Seq[org.scalajs.jsenv.Input])] {
            assert(ensureModuleKindIsCommonJSModule.value)
            val sjsOutput = fastOptJS.value.data
            // If jsdom is going to be used, then we should bundle the test module into a file that exports the tests to the global namespace
            if (requireJsDomEnv.value) Def.task {
              val logger = streams.value.log
              val targetDir = npmUpdate.value
              val sjsOutputName = sjsOutput.name.stripSuffix(".js")
              val bundle = targetDir / s"$sjsOutputName-bundle.js"
              val webpackVersion = (version in webpack).value

              val customWebpackConfigFile = (webpackConfigFile in Test).value
              val nodeArgs = (webpackNodeArgs in Test).value

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
                      NpmPackage(webpackVersion).major match {
                        case Some(4) =>
                          // TODO: It assumes tests are run on development mode. It should instead use build settings
                          Webpack.run(nodeArgs: _*)("--mode", "development", "--config", customConfigFileCopy.getAbsolutePath, loader.absolutePath, "--output", bundle.absolutePath)(targetDir, logger)
                        case _ =>
                          Webpack.run(nodeArgs: _*)("--config", customConfigFileCopy.getAbsolutePath, loader.absolutePath, bundle.absolutePath)(targetDir, logger)
                      }
                    case None =>
                      NpmPackage(webpackVersion).major match {
                        case Some(4) =>
                          // TODO: It assumes tests are run on development mode. It should instead use build settings
                          Webpack.run(nodeArgs: _*)("--mode", "development", loader.absolutePath, "--output", bundle.absolutePath)(targetDir, logger)
                        case _ =>
                          Webpack.run(nodeArgs: _*)(loader.absolutePath, bundle.absolutePath)(targetDir, logger)
                      }
                  }

                  Set.empty
                }
              writeTestBundleFunction(Set(sjsOutput))

              val jsdomDir = installJsdom.value
              val env = new JSDOMNodeJSEnv(JSDOMNodeJSEnv.Config(jsdomDir))
              val input = List(org.scalajs.jsenv.Input.Script(bundle.toPath))
              (env, input)
            } else Def.task {
              (jsEnv.value, jsEnvInput.value)
            }
          }.value
        }

        val configName = configuration.value.name

        if (fork.value) {
          throw new MessageOnlyException(
            s"`test in $configName` tasks in a Scala.js project require " +
              s"`fork in $configName := false`.")
        }

        if (!scalaJSUseTestModuleInitializer.value) {
          throw new MessageOnlyException(
            s"You may only use `test in $configName` tasks in " +
              "a Scala.js project if `scalaJSUseTestModuleInitializer in " +
              s"$configName := true`")
        }

        val frameworks = testFrameworks.value
        val frameworkNames = frameworks.map(_.implClassNames.toList).toList

        val logger = sbtLogger2ToolsLogger(streams.value.log)
        val config = TestAdapter.Config()
          .withLogger(logger)

        val adapter = newTestAdapter(env, input, config)
        val frameworkAdapters = adapter.loadFrameworks(frameworkNames)

        frameworks.zip(frameworkAdapters).collect {
          case (tf, Some(adapter)) => (tf, adapter)
        }.toMap: Map[TestFramework, Framework]
      }.dependsOn(npmUpdate).value
    )

  @tailrec
  private def newTestAdapter(jsEnv: JSEnv, input: Seq[org.scalajs.jsenv.Input], config: TestAdapter.Config): TestAdapter = {
    val prev = createdTestAdapters.get()
    val r = new TestAdapter(jsEnv, input, config)
    if (createdTestAdapters.compareAndSet(prev, r :: prev)) r
    else {
      r.close()
      newTestAdapter(jsEnv, input, config)
    }
  }

}
