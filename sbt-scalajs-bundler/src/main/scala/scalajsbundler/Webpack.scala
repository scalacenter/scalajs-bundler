package scalajsbundler

import sbt._
import scalajsbundler.util.{Commands, JS}
import org.scalajs.core.tools.linker.StandardLinker.Config
import java.io.InputStream

import play.api.libs.json._
import Stats._

import scala.util.{Failure, Success, Try}

object Webpack {
  // Represents webpack 4 modes
  sealed trait WebpackMode {
    def mode: String
  }
  case object DevelopmentMode extends WebpackMode {
    val mode = "development"
  }
  case object ProductionMode extends WebpackMode {
    val mode = "production"
  }
  object WebpackMode {
    def apply(sjsConfig: Config): WebpackMode = {
      if (sjsConfig.semantics.productionMode) {
        ProductionMode
      } else {
        DevelopmentMode
      }
    }
  }

  /**
    * Copies the custom webpack configuration file and the webpackResources to the target dir
    *
    * @param targetDir target directory
    * @param webpackResources Resources to copy
    * @param customConfigFile User supplied config file
    * @return The copied config file.
    */
  def copyCustomWebpackConfigFiles(targetDir: File, webpackResources: Seq[File])(customConfigFile: File): File = {
    def copyToWorkingDir(targetDir: File)(file: File): File = {
      val copy = targetDir / file.name
      IO.copyFile(file, copy)
      copy
    }

    webpackResources.foreach(copyToWorkingDir(targetDir))
    copyToWorkingDir(targetDir)(customConfigFile)
  }

  /**
    * Writes the webpack configuration file. The output file is designed to be minimal, and to be extended,
    * however, the `entry` and `output` keys must be preserved in order for the bundler to work as expected.
    *
    * @param emitSourceMaps Whether source maps is enabled at all
    * @param entry The input entrypoint file to process via webpack
    * @param webpackConfigFile webpack configuration file to write to
    * @param libraryBundleName If defined, generate a library bundle named `libraryBundleName`
    * @param log Logger
    */
  def writeConfigFile(
    emitSourceMaps: Boolean,
    entry: BundlerFile.WebpackInput,
    webpackConfigFile: BundlerFile.WebpackConfig,
    libraryBundleName: Option[String],
    mode: WebpackMode,
    log: Logger
  ): Unit = {
    log.info("Writing scalajs.webpack.config.js")
    // Build the output configuration, configured for library output
    // if a library bundle name is provided
    val output = libraryBundleName match {
      case Some(bundleName) =>
        JS.obj(
          "path" -> JS.str(webpackConfigFile.targetDir.toAbsolutePath.toString),
          "filename" -> JS.str(BundlerFile.Library.fileName("[name]")),
          "library" -> JS.str(bundleName),
          "libraryTarget" -> JS.str("var")
        )
      case None =>
        JS.obj(
          "path" -> JS.str(webpackConfigFile.targetDir.toAbsolutePath.toString),
          "filename" -> JS.str(BundlerFile.ApplicationBundle.fileName("[name]"))
        )
    }

    // Build the file itself
    val webpackConfigContent =
      JS.ref("module").dot("exports").assign(JS.obj(Seq(
        "entry" -> JS.obj(
          entry.project -> JS.arr(JS.str(entry.file.absolutePath))
        ),
        "output" -> output
      ) ++ (
        if (emitSourceMaps) {
          val webpackNpmPackage = NpmPackage.getForModule(webpackConfigFile.targetDir.toFile, "webpack")
          webpackNpmPackage.flatMap(_.major) match {
            case Some(1) =>
              Seq(
                "devtool" -> JS.str("source-map"),
                "module" -> JS.obj(
                  "preLoaders" -> JS.arr(
                    JS.obj(
                      "test" -> JS.regex("\\.js$"),
                      "loader" -> JS.str("source-map-loader")
                    )
                  )
                )
              )
            case Some(2) =>
              Seq(
                "devtool" -> JS.str("source-map"),
                "module" -> JS.obj(
                  "rules" -> JS.arr(
                    JS.obj(
                      "test" -> JS.regex("\\.js$"),
                      "enforce" -> JS.str("pre"),
                      "loader" -> JS.str("source-map-loader")
                    )
                  )
                )
              )
            case Some(3) =>
              Seq(
                "devtool" -> JS.str("source-map"),
                "module" -> JS.obj(
                  "rules" -> JS.arr(
                    JS.obj(
                      "test" -> JS.regex("\\.js$"),
                      "enforce" -> JS.str("pre"),
                      "use" -> JS.arr(JS.str("source-map-loader"))
                    )
                  )
                )
              )
            case Some(4) =>
              Seq(
                "mode" -> JS.str(mode.mode),
                "devtool" -> JS.str("source-map"),
                "module" -> JS.obj(
                  "rules" -> JS.arr(
                    JS.obj(
                      "test" -> JS.regex("\\.js$"),
                      "enforce" -> JS.str("pre"),
                      "use" -> JS.arr(JS.str("source-map-loader"))
                    )
                  )
                )
              )
            case Some(x) => sys.error(s"Unsupported webpack major version $x")
            case None => sys.error("No webpack version defined")
          }
        } else Nil
        ): _*))
    IO.write(webpackConfigFile.file, webpackConfigContent.show)
  }

  /**
    * Run webpack to bundle the application.
    *
    * @param emitSourceMaps Whether or not source maps are enabled
    * @param generatedWebpackConfigFile Webpack config file generated by scalajs-bundler
    * @param customWebpackConfigFile User supplied config file
    * @param webpackResources Additional resources to be copied to the working folder
    * @param entry Scala.js application to bundle
    * @param targetDir Target directory (and working directory for Nodejs)
    * @param extraArgs Extra arguments passed to webpack
    * @param mode Mode for webpack 4
    * @param log Logger
    * @return The generated bundles
    */
  def bundle(
     emitSourceMaps: Boolean,
     generatedWebpackConfigFile: BundlerFile.WebpackConfig,
     customWebpackConfigFile: Option[File],
     webpackResources: Seq[File],
     entry: BundlerFile.Application,
     targetDir: File,
     extraArgs: Seq[String],
     nodeArgs: Seq[String],
     mode: WebpackMode,
     log: Logger
  ): BundlerFile.ApplicationBundle = {
    writeConfigFile(emitSourceMaps, entry, generatedWebpackConfigFile, None, mode, log)

    val configFile = customWebpackConfigFile
      .map(Webpack.copyCustomWebpackConfigFiles(targetDir, webpackResources))
      .getOrElse(generatedWebpackConfigFile.file)

    log.info("Bundling the application with its NPM dependencies")
    val args = extraArgs ++: Seq("--config", configFile.absolutePath)
    val stats = Webpack.run(nodeArgs: _*)(args: _*)(targetDir, log)
    stats.foreach(_.print(log))

    // Attempt to discover the actual name produced by webpack indexing by chunk name and discarding maps
    val bundle = generatedWebpackConfigFile.asApplicationBundle(stats)
    assert(bundle.file.exists(), "Webpack failed to create application bundle")
    assert(bundle.assets.forall(_.exists()), "Webpack failed to create application assets")
    bundle
  }

  /**
    * Run webpack to bundle the application.
    *
    * @param emitSourceMaps Are source maps enabled?
    * @param generatedWebpackConfigFile Webpack config file generated by scalajs-bundler
    * @param customWebpackConfigFile User supplied config file
    * @param webpackResources Additional webpack resources to include in the working directory
    * @param entryPointFile The entrypoint file to bundle dependencies for
    * @param libraryModuleName The library module name to assign the webpack bundle to
    * @param extraArgs Extra arguments passed to webpack
    * @param mode Mode for webpack 4
    * @param log Logger
    * @return The generated bundle
    */
  def bundleLibraries(
    emitSourceMaps: Boolean,
    generatedWebpackConfigFile: BundlerFile.WebpackConfig,
    customWebpackConfigFile: Option[File],
    webpackResources: Seq[File],
    entryPointFile: BundlerFile.EntryPoint,
    libraryModuleName: String,
    extraArgs: Seq[String],
    nodeArgs: Seq[String],
    mode: WebpackMode,
    log: Logger
  ): BundlerFile.Library = {
    writeConfigFile(
      emitSourceMaps,
      entryPointFile,
      generatedWebpackConfigFile,
      Some(libraryModuleName),
      mode,
      log
    )

    val configFile = customWebpackConfigFile
      .map(Webpack.copyCustomWebpackConfigFiles(generatedWebpackConfigFile.targetDir.toFile, webpackResources))
      .getOrElse(generatedWebpackConfigFile.file)

    val args = extraArgs ++: Seq("--config", configFile.absolutePath)
    val stats = Webpack.run(nodeArgs: _*)(args: _*)(generatedWebpackConfigFile.targetDir.toFile, log)
    stats.foreach(_.print(log))

    val library = generatedWebpackConfigFile.asLibrary(stats)
    assert(library.file.exists, "Webpack failed to create library file")
    assert(library.assets.forall(_.exists), "Webpack failed to create library assets")
    library
  }

  private def jsonOutput(cmd: Seq[String], logger: Logger)(in: InputStream): Option[WebpackStats] = {
    Try {
      val parsed = Json.parse(in)
      parsed.validate[WebpackStats] match {
        case JsError(e) =>
          logger.error("Error parsing webpack stats output")
          // In case of error print the result and return None. it will be ignored upstream
          e.foreach {
            case (p, v) => logger.error(s"$p: ${v.mkString(",")}")
          }
          None
        case JsSuccess(p, _) =>
          if (p.warnings.nonEmpty || p.errors.nonEmpty) {
            logger.info("")
            // Filtering is a workaround for #111
            p.warnings.filterNot(_.contains("https://raw.githubusercontent.com")).foreach(x => logger.warn(x))
            p.errors.foreach(x => logger.error(x))
          }
          Some(p)
      }
    } match {
      case Success(x) =>
        x
      case Failure(e) =>
        // In same cases errors are not reported on the json output but comes on stdout
        // where they cannot be parsed as json. The best we can do here is to suggest
        // running the command manually
        logger.error(s"Failure on parsing the output of webpack: ${e.getMessage}")
        logger.error(s"You can try to manually execute the command")
        logger.error(cmd.mkString(" "))
        logger.error("\n")
        None
    }
  }

  /**
    * Runs the webpack command.
    *
    * @param nodeArgs node.js cli flags
    * @param args Arguments to pass to the webpack command
    * @param workingDir Working directory in which the Nodejs will be run (where there is the `node_modules` subdirectory)
    * @param log Logger
    */
  def run(nodeArgs: String*)(args: String*)(workingDir: File, log: Logger): Option[WebpackStats] = {
    val webpackBin = workingDir / "node_modules" / "webpack" / "bin" / "webpack"
    val params = nodeArgs ++ Seq(webpackBin.absolutePath, "--bail", "--profile", "--json") ++ args
    val cmd = "node" +: params
    Commands.run(cmd, workingDir, log, jsonOutput(cmd, log)).fold(sys.error, _.flatten)
  }

}
