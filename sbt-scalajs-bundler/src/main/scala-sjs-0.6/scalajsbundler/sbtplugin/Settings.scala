package scalajsbundler.sbtplugin

import org.scalajs.core.tools.io.FileVirtualJSFile
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.core.tools.linker.backend.ModuleKind
import org.scalajs.jsenv.ComJSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.sbtplugin.Loggers.sbtLogger2ToolsLogger
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.{fastOptJS, jsEnv, loadedJSEnv, scalaJSConsole, scalaJSModuleKind}
import org.scalajs.sbtplugin.ScalaJSPluginInternal.{scalaJSEnsureUnforked, scalaJSModuleIdentifier}
import sbt.Keys.{loadedTestFrameworks, streams, testFrameworks, version}
import sbt._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.{installJsdom, npmUpdate, requireJsDomEnv, webpack, webpackConfigFile, webpackNodeArgs, webpackResources}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.{createdTestAdapters, ensureModuleKindIsNotNoModule}
import scalajsbundler.scalajs.compat.testing.TestAdapter
import scalajsbundler.{JSDOMNodeJSEnv, JsDomTestEntries, NpmPackage, Webpack}

import scala.annotation.tailrec

private[sbtplugin] object Settings {

  // Override Scala.js’ jsEnvInput to first run `npm update`
  val jsEnvSetting =
    loadedJSEnv := loadedJSEnv.dependsOn(npmUpdate).value

  // Override Scala.js setting, which does not support the combination of jsdom and CommonJS module output kind
  val loadedTestFrameworksSetting =
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
            assert(ensureModuleKindIsNotNoModule.value)
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

        if (requireJsDomEnv.value) (ModuleKind.NoModule, None)
        else withoutDom
      }

      val frameworkNames = frameworks.map(_.implClassNames.toList).toList

      val config = TestAdapter.Config()
        .withLogger(toolsLogger)
        .withJSConsole(console)
        .withModuleSettings(moduleKind, moduleIdentifier)

      val adapter = newTestAdapter(env, config)
      val frameworkAdapters = adapter.loadFrameworks(frameworkNames)

      frameworks.zip(frameworkAdapters).collect {
        case (tf, Some(adapter)) => (tf, adapter)
      }.toMap
    }.dependsOn(npmUpdate).value

  @tailrec
  private def newTestAdapter(jsEnv: ComJSEnv, config: TestAdapter.Config): TestAdapter = {
    val prev = createdTestAdapters.get()
    val r = new TestAdapter(jsEnv, config)
    if (createdTestAdapters.compareAndSet(prev, r :: prev)) r
    else {
      r.close()
      newTestAdapter(jsEnv, config)
    }
  }

}
