package scalajsbundler.sbtplugin

import scala.annotation.tailrec

import org.scalajs.jsenv.JSEnv
import org.scalajs.sbtplugin.Loggers.sbtLogger2ToolsLogger
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{fastOptJS, jsEnv, jsEnvInput, scalaJSUseTestModuleInitializer}
import sbt.Keys.{configuration, fork, loadedTestFrameworks, streams, testFrameworks, version}
import sbt._
import sbt.testing.Framework
import scalajsbundler.{JSDOMNodeJSEnv, Webpack, JsDomTestEntries, NpmPackage}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.{installJsdom, npmUpdate, requireJsDomEnv, webpackConfigFile, webpackNodeEnvVars, webpackNodeArgs, webpackResources, webpack}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.{createdTestAdapters, ensureModuleKindIsCommonJSModule}
import scalajsbundler.scalajs.compat.testing.TestAdapter

private[sbtplugin] object Settings {

  // Override Scala.js’ jsEnvInput to first run `npm update`
  val jsEnvSetting =
    jsEnvInput := jsEnvInput.dependsOn(npmUpdate).value

  // Override Scala.js setting, which does not support the combination of jsdom and CommonJS module output kind
  val loadedTestFrameworksSetting =
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
            val nodeEnvVars = (webpackNodeEnvVars in Test).value.toSeq
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
                        Webpack.run(nodeEnvVars: _*)(nodeArgs: _*)("--mode", "development", "--config", customConfigFileCopy.getAbsolutePath, loader.absolutePath, "--output", bundle.absolutePath)(targetDir, logger)
                      case _ =>
                        Webpack.run(nodeEnvVars: _*)(nodeArgs: _*)("--config", customConfigFileCopy.getAbsolutePath, loader.absolutePath, bundle.absolutePath)(targetDir, logger)
                    }
                  case None =>
                    NpmPackage(webpackVersion).major match {
                      case Some(4) =>
                        // TODO: It assumes tests are run on development mode. It should instead use build settings
                        Webpack.run(nodeEnvVars: _*)(nodeArgs: _*)("--mode", "development", loader.absolutePath, "--output", bundle.absolutePath)(targetDir, logger)
                      case _ =>
                        Webpack.run(nodeEnvVars: _*)(nodeArgs: _*)(loader.absolutePath, bundle.absolutePath)(targetDir, logger)
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
