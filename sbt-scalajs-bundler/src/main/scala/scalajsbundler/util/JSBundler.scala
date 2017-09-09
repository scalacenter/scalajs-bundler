package scalajsbundler.util

import sbt._

import scalajsbundler.BundlerFile

object JSBundler {

  def loaderScript(bundleName: String): String =
    s"""
       |var exports = window;
       |exports.require = window["$bundleName"].require;
    """.stripMargin

  def writeLoader(
      loaderFile: BundlerFile.Loader,
      bundleName: String
  ): Unit =
    IO.write(loaderFile.file, loaderScript(bundleName))

  /**
    * Run webpack to bundle the application.
    *
    * @param targetDir Target directory (and working directory for Nodejs)
    * @param logger Logger
    * @return The generated bundles
    */
  def bundle(
      targetDir: File,
      entry: BundlerFile.Application,
      libraryFile: BundlerFile.Library,
      emitSourceMaps: Boolean = false,
      libraryBundleName: String,
      logger: Logger
  ): BundlerFile.ApplicationBundle = {
    val bundleFile = entry.asApplicationBundle
    val loaderFile = entry.asLoader
    writeLoader(loaderFile, libraryBundleName)
    if (emitSourceMaps) {
      logger.info("Bundling dependencies with source maps")
      val concatContent =
        JS.let(
          JS.ref("require")(JS.str("concat-with-sourcemaps")),
          JS.ref("require")(JS.str("fs"))
        ) { (Concat, fs) =>
          JS.let(
            JS.`new`(Concat,
                     JS.bool(true),
                     JS.str(bundleFile.file.name),
                     JS.str(";\n"))) { concat =>
            JS.block(
              concat
                .dot("add")
                .apply(
                  JS.str(""),
                  fs.dot("readFileSync")
                    .apply(JS.str(libraryFile.file.absolutePath),
                           JS.str("utf-8")),
                  fs.dot("readFileSync")
                    .apply(JS.str(libraryFile.file.absolutePath ++ ".map"))
                ),
              concat
                .dot("add")
                .apply(JS.str(loaderFile.file.name),
                       fs.dot("readFileSync")
                         .apply(JS.str(loaderFile.file.absolutePath))),
              concat
                .dot("add")
                .apply(
                  JS.str(""),
                  fs.dot("readFileSync")
                    .apply(JS.str(entry.file.absolutePath), JS.str("utf-8")),
                  fs.dot("readFileSync")
                    .apply(JS.str(entry.file.absolutePath ++ ".map"),
                           JS.str("utf-8"))
                ),
              JS.let(JS.`new`(
                JS.ref("Buffer"),
                JS.str(
                  s"\n//# sourceMappingURL=${bundleFile.file.name ++ ".map"}\n"))) {
                endBuffer =>
                  JS.let(
                    JS.ref("Buffer")
                      .dot("concat")
                      .apply(JS.arr(concat.dot("content"), endBuffer))) {
                    result =>
                      fs.dot("writeFileSync")
                        .apply(JS.str(bundleFile.file.absolutePath), result)
                  }
              },
              fs.dot("writeFileSync")
                .apply(JS.str(bundleFile.file.absolutePath ++ ".map"),
                       concat.dot("sourceMap"))
            )
          }
        }
      val concatFile = targetDir / s"scalajsbundler-concat-${bundleFile.file.name}.js"
      IO.write(concatFile, concatContent.show)
      Commands.run(Seq("node", concatFile.absolutePath), targetDir, logger)
    } else {
      logger.info("Bundling dependencies without source maps")
      IO.withTemporaryFile("scalajs-bundler", entry.project) { tmpFile =>
        IO.append(tmpFile, IO.readBytes(libraryFile.file))
        IO.append(tmpFile, "\n")
        IO.append(tmpFile, IO.readBytes(loaderFile.file))
        IO.append(tmpFile, "\n")
        IO.append(tmpFile, IO.readBytes(entry.file))
        IO.move(tmpFile, bundleFile.file)
      }
    }
    bundleFile
  }
}
