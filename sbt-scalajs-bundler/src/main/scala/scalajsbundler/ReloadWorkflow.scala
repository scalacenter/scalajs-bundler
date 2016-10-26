package scalajsbundler

import org.scalajs.core.ir.Trees.JSNativeLoadSpec
import org.scalajs.core.tools.io.VirtualScalaJSIRFile
import org.scalajs.core.tools.linker.ClearableLinker
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend, OutputMode}
import org.scalajs.sbtplugin.Loggers
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import sbt._

import JS.syntax._

/**
  * Faster workflow when a developer changes the source code and then reloads the application.
  *
  * It pre-bundles the dependencies once and then recompiles just the part of the application
  * that changed.
  *
  * For this to work, the pre-bundled dependencies are exposed to the global namespace and a fake `require`
  * implementation is also provided so that the output of Scala.js, which is supposed to be executed by
  * a CommonJS compatible environment, can be executed by a web browser.
  */
object ReloadWorkflow {

  /**
    * @return A file that contains the concatenation of the dependencies bundle and the output of the Scala.js compilation
    * @param bundledDependencies Pre-bundled dependencies
    * @param loaderAndLauncher `require` implementation and entry point launcher
    * @param sjsOutput Output of Scala.js
    * @param targetDir Target directory (where to write the bundle)
    */
  def writeFakeBundle(
    bundledDependencies: File,
    loaderAndLauncher: LoaderAndLauncher,
    sjsOutput: File,
    targetDir: File
  ): File = {
    val moduleName = sjsOutput.name.stripSuffix(".js")
    val bundle = targetDir / s"$moduleName-bundle.js" // Because scalajs.webpack.config.js defines the output as "[name]-bundle.js"
    IO.copyFile(bundledDependencies, bundle)
    IO.append(bundle, "\n")
    IO.append(bundle, IO.readBytes(loaderAndLauncher.loader)) // shims `require`
    IO.append(bundle, ";\n")
    IO.append(bundle, IO.readBytes(sjsOutput))
    IO.append(bundle, "\n")
    IO.append(bundle, IO.readBytes(loaderAndLauncher.launcher))
    bundle
  }

  /**
    * @return The written loader (faking a `require` implementation) and launcher files
    * @param mainClass Application entry point
    * @param targetDir Target directory
    */
  def writeLoaderAndLauncher(mainClass: String, targetDir: File): LoaderAndLauncher = {
    val depsLoader = targetDir / "scalajsbundler-deps-loader.js"
    val window = JS.ref("window")
    val depsLoaderContent =
      JS.block(
        (window `.` "require") := JS.fun(name => window.bracket((JS.str(modulePrefix) `.` "concat")(name))),
        (window `.` "exports") := JS.obj()
      )
    IO.write(depsLoader, depsLoaderContent.show)

    val launcher = targetDir / "scalajsbundler-launcher.js"
    val launcherContent = Launcher.callEntryPoint(mainClass, window `.` "exports")
    IO.write(launcher, launcherContent.show)

    LoaderAndLauncher(depsLoader, launcher)
  }

  /**
    * @return The imported dependencies of the Scala.js project, bundled into a single file, and exported to the
    *         global namespace
    * @param linker Scala.js linker
    * @param irFiles Scala.js IR files
    * @param outputMode Scala.js output mode
    * @param emitSourceMaps Whether emitSourceMaps is enabled
    * @param targetDir Target directory (also the directory where we expect node_modules to be present)
    * @param logger Logger
    */
  def bundleDependencies(
    linker: ClearableLinker,
    irFiles: Seq[VirtualScalaJSIRFile],
    outputMode: OutputMode,
    emitSourceMaps: Boolean,
    targetDir: File,
    logger: Logger
  ): File = {
    val imports = findImportedModules(linker, irFiles, outputMode, emitSourceMaps, logger)
    val depsFileContent =
      JS.block(
        imports.map { moduleName =>
          JS.ref("global").bracket(s"$modulePrefix$moduleName") := JS.ref("require")(JS.str(moduleName))
        }: _*
      )
    val dependenciesCjsFile = targetDir / "scalajsbundler-deps-cjs.js"
    IO.write(dependenciesCjsFile, depsFileContent.show)

    val dependenciesFile = targetDir / "scalajsbundler-deps.js"
    val webpackBin = targetDir / "node_modules" / "webpack" / "bin" / "webpack"
    Commands.run(
      s"node ${webpackBin.absolutePath} ${dependenciesCjsFile.absolutePath} ${dependenciesFile.absolutePath}",
      targetDir,
      logger
    )

    dependenciesFile
  }

  /**
    * @return The list of ES modules imported by a Scala.js project
    * @param linker Scala.js linker
    * @param irFiles Scala.js IR files
    * @param outputMode Scala.js output mode
    * @param emitSourceMaps Whether emitSourceMaps is enabled
    * @param logger Logger
    */
  def findImportedModules(
    linker: ClearableLinker,
    irFiles: Seq[VirtualScalaJSIRFile],
    outputMode: OutputMode,
    emitSourceMaps: Boolean,
    logger: Logger
  ): List[String] = {
    val semantics = linker.semantics
    val symbolRequirements =
      new BasicLinkerBackend(semantics, outputMode, ModuleKind.CommonJSModule, emitSourceMaps, LinkerBackend.Config())
        .symbolRequirements
    val linkingUnit =
      linker.linkUnit(irFiles, symbolRequirements, Loggers.sbtLogger2ToolsLogger(logger))
    linkingUnit.classDefs.flatMap(_.jsNativeLoadSpec).collect {
      case JSNativeLoadSpec.Import(module, _) => module
    }.distinct
  }

  // Prefix to use to export/import pre-bundled modules to/from the global namespace
  val modulePrefix = "__scalajsbundler__"

  case class LoaderAndLauncher(loader: File, launcher: File)

}
