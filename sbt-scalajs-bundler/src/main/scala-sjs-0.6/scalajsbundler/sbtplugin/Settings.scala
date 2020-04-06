package scalajsbundler.sbtplugin

import java.util.concurrent.atomic.AtomicReference
import org.scalajs.core.tools.io.{FileVirtualJSFile, VirtualScalaJSIRFile}
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.core.tools.linker.{ClearableLinker, LinkingUnit, ModuleInitializer, StandardLinker}
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend, ModuleKind}
import org.scalajs.jsenv.ComJSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.sbtplugin.Loggers.sbtLogger2ToolsLogger
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPluginInternal.{scalaJSEnsureUnforked, scalaJSModuleIdentifier}
import org.scalajs.testadapter.TestAdapter
import sbt.Keys._
import sbt._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.{installJsdom, npmUpdate, requireJsDomEnv, webpack, webpackConfigFile, webpackNodeArgs, webpackResources}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.{ensureModuleKindIsCommonJSModule, scalaJSBundlerImportedModules}
import scalajsbundler.{JSDOMNodeJSEnv, JsDomTestEntries, NpmPackage, Webpack}

import scala.annotation.tailrec

private[sbtplugin] object Settings {

  private val createdTestAdapters = new AtomicReference[List[TestAdapter]](Nil)

  private def closeAllTestAdapters(): Unit =
    createdTestAdapters.getAndSet(Nil).foreach(_.close())

  // Settings that must be applied in Global
  val globalSettings: Seq[Setting[_]] =
    Def.settings(
      onComplete := {
        val prev = onComplete.value

        { () =>
          prev()
          closeAllTestAdapters()
        }
      }
    )

  // Settings that must be applied for each stage in each configuration
  private def scalaJSStageSettings(key: TaskKey[Attributed[File]]): Seq[Setting[_]] =
    Def.settings(
      scalaJSBundlerImportedModules in key := {
        Def.taskDyn {
          val s = streams.value
          val linkerConfig = (scalaJSLinkerConfig in key).value
          val linker = (scalaJSLinker in key).value
          val linkerTag = (usesScalaJSLinkerTag in key).value
          val scalaJSIRValue = scalaJSIR.value
          val scalaJSModuleInitializersValue = scalaJSModuleInitializers.value
          Def
            .task {
              val linkingUnit = ScalaJSOutputAnalyzer.linkingUnit(
                linkerConfig,
                linker,
                scalaJSIRValue.data,
                scalaJSModuleInitializersValue,
                s.log
              )
              ScalaJSOutputAnalyzer.importedModules(linkingUnit)
            }
            .tag(linkerTag)
            .dependsOn(npmUpdate in key)
        }.value
      }
    )

  // Settings that must be applied for each configuration
  val configSettings: Seq[Setting[_]] =
    Def.settings(
      // Override Scala.js’ loadedJSEnv to first run `npm update`
      loadedJSEnv := loadedJSEnv.dependsOn(npmUpdate).value,

      scalaJSStageSettings(fastOptJS),
      scalaJSStageSettings(fullOptJS)
    )

  // Settings that must be applied in the Test configuration
  val testConfigSettings: Seq[Setting[_]] =
    Def.settings(
      // Override loadedTestFrameworks from Scala.js, which does not support the combination of jsdom and CommonJS module output kind
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
    )

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

  private object ScalaJSOutputAnalyzer {

    /**
      * @return The list of ES modules imported by a Scala.js project
      * @param linkingUnit The Scala.js linking unit
      */
    def importedModules(linkingUnit: LinkingUnit): List[String] = {
      import org.scalajs.core.ir.Trees.JSNativeLoadSpec._
      linkingUnit.classDefs
        .flatMap(_.jsNativeLoadSpec)
        .flatMap {
          case Import(module, _) => List(module)
          case ImportWithGlobalFallback(Import(module, _), _) => List(module)
          case Global(_) => Nil
        }
        .distinct
    }

    /**
      * Extract the linking unit from the Scala.js output
      *
      * @param linkerConfig Configuration of the Scala.js linker
      * @param linker Scala.js linker
      * @param irFiles Scala.js IR files
      * @param moduleInitializers Scala.js module initializers
      * @param logger Logger
      * @return
      */
    def linkingUnit(
        linkerConfig: StandardLinker.Config,
        linker: ClearableLinker,
        irFiles: Seq[VirtualScalaJSIRFile],
        moduleInitializers: Seq[ModuleInitializer],
        logger: Logger
    ): LinkingUnit = {
      val symbolRequirements = {
        val backend = new BasicLinkerBackend(linkerConfig.semantics,
                                             linkerConfig.esFeatures,
                                             linkerConfig.moduleKind,
                                             linkerConfig.sourceMap,
                                             LinkerBackend.Config())
        backend.symbolRequirements
      }
      linker.linkUnit(irFiles,
                      moduleInitializers,
                      symbolRequirements,
                      sbtLogger2ToolsLogger(logger))
    }

  }

}
