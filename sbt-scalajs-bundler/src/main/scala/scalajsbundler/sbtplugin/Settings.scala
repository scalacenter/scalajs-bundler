package scalajsbundler.sbtplugin

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._
import org.scalajs.jsenv.Input._
import org.scalajs.linker.interface._
import org.scalajs.sbtplugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import scalajsbundler.{JSDOMNodeJSEnv, Webpack, JsDomTestEntries, NpmPackage}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.{installJsdom, npmUpdate, requireJsDomEnv, webpackConfigFile, webpackNodeArgs, webpackResources, webpack}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.{ensureModuleKindIsCommonJSModule, scalaJSBundlerImportedModules}
import scalajsbundler.sbtplugin.internal.BuildInfo

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
        val dummyModuleID =
          "ch.epfl.scala" % "scalajs-bundler-linker-and-scalajs-linker_2.12" % s"${BuildInfo.version}-$scalaJSVersion"
        val dependencies = Vector(
            // Load our linker back-end
            "ch.epfl.scala" % "scalajs-bundler-linker_2.12" % BuildInfo.version,
            // And force-bump the dependency on scalajs-linker to match the version of sbt-scalajs
            "org.scala-js" % "scalajs-linker_2.12" % scalaJSVersion
        )
        val moduleDescriptor = lm.moduleDescriptor(dummyModuleID, dependencies, scalaModuleInfo = None)
        lm.retrieve(moduleDescriptor, retrieveDir, log)
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
  private def scalaJSStageSettings(stage: Stage, key: TaskKey[Attributed[Report]],
      legacyKey: TaskKey[Attributed[File]]): Seq[Setting[_]] = {
    val entryPointOutputFileName =
      s"entrypoints-${stage.toString.toLowerCase}.txt"

    Def.settings(
      scalaJSLinker in legacyKey := {
        val config = (scalaJSLinkerConfig in key).value
        val box = (scalaJSLinkerBox in key).value
        val linkerImpl = (scalaJSLinkerImpl in key).value
        val entryPointOutputFile = crossTarget.value / entryPointOutputFileName

        box.ensure {
          linkerImpl.asInstanceOf[BundlerLinkerImpl]
            .bundlerLinker(config, entryPointOutputFile.toPath)
        }
      },

      scalaJSBundlerImportedModules in legacyKey := {
        val _ = legacyKey.value
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

      /* Moreover, force it to use the output of the legacy key, because lots
       * of things in scalajs-bundler assume that there is only one .js file
       * that we can put in a specific directory to make things work.
       */
      jsEnvInput := {
        val prev = jsEnvInput.value
        val linkingResult = scalaJSLinkerResult.value
        val legacyKeyOutput = scalaJSLinkedFile.value

        // Compute the path to the `main` module, which is what sbt-scalajs puts in jsEnvInput
        val report = linkingResult.data
        val optMainModule = report.publicModules.find(_.moduleID == "main")
        val optMainModulePath = optMainModule.map { mainModule =>
          val linkerOutputDirectory = linkingResult.get(scalaJSLinkerOutputDirectory.key).getOrElse {
            throw new MessageOnlyException(
                "Linking report was not attributed with output directory. " +
                "Please report this as a Scala.js bug.")
          }
          (linkerOutputDirectory / mainModule.jsFileName).toPath
        }

        // Replace the path to the `main` module by the path to the legacy key output
        optMainModulePath match {
          case Some(mainModulePath) =>
            prev.map {
              case CommonJSModule(module) if module == mainModulePath =>
                CommonJSModule(legacyKeyOutput.data.toPath())
              case inputItem =>
                inputItem
            }
          case None =>
            prev
        }
      },

      scalaJSStageSettings(FastOptStage, fastLinkJS, fastOptJS),
      scalaJSStageSettings(FullOptStage, fullLinkJS, fullOptJS)
    )

  // Settings that must be applied in the Test configuration
  val testConfigSettings: Seq[Setting[_]] =
    Def.settings(
      // Configure a JSDOMNodeJSEnv with an installation of jsdom if requireJsDomEnv is true
      jsEnv := {
        val defaultJSEnv = jsEnv.value
        val optJsdomDir = Def.taskDyn[Option[File]] {
          if (requireJsDomEnv.value)
            installJsdom.map(Some(_))
          else
            Def.task(None)
        }.value
        optJsdomDir match {
          case Some(jsdomDir) => new JSDOMNodeJSEnv(JSDOMNodeJSEnv.Config(jsdomDir))
          case None           => defaultJSEnv
        }
      },

      // Use the product of bundling in jsEnvInput if requireJsDomEnv is true
      jsEnvInput := Def.task {
        assert(ensureModuleKindIsCommonJSModule.value)
        val prev = jsEnvInput.value
        val sjsOutput = scalaJSLinkedFile.value.data

        val optBundle = {
          Def.taskDyn[Option[org.scalajs.jsenv.Input]] {
            val sjsOutput = scalaJSLinkedFile.value.data
            // If jsdom is going to be used, then we should bundle the test module
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

                  val configArgs = customWebpackConfigFile match {
                    case Some(configFile) =>
                      val customConfigFileCopy = Webpack.copyCustomWebpackConfigFiles(targetDir, webpackResources.value.get)(configFile)
                      Seq("--config", customConfigFileCopy.getAbsolutePath)

                    case None =>
                      Seq.empty
                  }

                  // TODO: It assumes tests are run on development mode. It should instead use build settings
                  val allArgs = Seq(
                    "--mode", "development",
                    "--entry", loader.absolutePath,
                    "--output-path", bundle.getParentFile.absolutePath,
                    "--output-filename", bundle.name
                  ) ++ configArgs

                  NpmPackage(webpackVersion).major match {
                    case Some(5) =>
                      Webpack.run(nodeArgs: _*)(allArgs: _*)(targetDir, logger)
                    case Some(x) =>
                      sys.error(s"Unsupported webpack major version $x")
                    case None =>
                      sys.error("No webpack version defined")
                  }

                  Set.empty
                }
              writeTestBundleFunction(Set(sjsOutput))

              Some(Script(bundle.toPath))
            } else Def.task {
              None
            }
          }.value
        }

        optBundle match {
          case Some(bundle) =>
            prev.map {
              case CommonJSModule(module) if module == sjsOutput.toPath() =>
                bundle
              case inputItem =>
                inputItem
            }
          case None =>
            prev
        }
      }.dependsOn(npmUpdate).value
    )

}
