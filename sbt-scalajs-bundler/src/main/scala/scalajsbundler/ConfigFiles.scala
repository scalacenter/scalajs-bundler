package scalajsbundler

import sbt._

/**
  * Configuration files written by scalajs-bundler
  * @param webpackConfig Webpack configuration file
  * @param packageJson package.json
  * @param output Expected output files from Webpack
  */
case class ConfigFiles(
  webpackConfig: File,
  packageJson: File,
  output: Seq[File]
)

object ConfigFiles {

  import JS.syntax._

  def writeConfigFiles(
    log: Logger,
    targetDir: File,
    webpackVersion: String,
    customWebpackConfig: Option[File],
    webpackEntries: Seq[(String, File)],
    emitSourceMaps: Boolean,
    fullClasspath: Seq[Attributed[File]],
    npmDependencies: Seq[(String, String)],
    npmDevDependencies: Seq[(String, String)],
    currentConfiguration: Configuration
  ): ConfigFiles = {
      // Create scalajs.webpack.config.js
      val scalajsConfigFile = targetDir / "scalajs.webpack.config.js"
      val scalajsConfigContent =
        JS.ref("module") `.` "exports" := JS.obj(Seq(
          "entry" -> JS.obj(webpackEntries.map { case (key, file) =>
            key -> JS.str(file.absolutePath) }: _*
          ),
          "output" -> JS.obj(
            "path" -> JS.str(targetDir.absolutePath),
            "filename" -> JS.str("[name]-bundle.js")
          )
        ) ++ (
          if (emitSourceMaps) {
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
          } else Nil
        ): _*)
      log.debug("Writing 'scalajs.webpack.config.js'")
      IO.write(scalajsConfigFile, scalajsConfigContent.show)

      // Create a package.json file
      val bundleCommand =
      customWebpackConfig match {
        case Some(configFile) =>
          val configFileCopy = targetDir / configFile.name
          IO.copyFile(configFile, configFileCopy)
          s"webpack --config ${configFileCopy.absolutePath}"
        case None =>
          s"webpack --config ${scalajsConfigFile.absolutePath}"
      }

      val npmManifestDependencies = NpmDependencies.collectFromClasspath(fullClasspath)
      val dependencies =
        npmDependencies ++ (
          if (currentConfiguration == Compile) npmManifestDependencies.compileDependencies
          else npmManifestDependencies.testDependencies
        )
      val devDependencies =
        Seq("webpack" -> webpackVersion) ++ (
          if (currentConfiguration == Compile) npmManifestDependencies.compileDevDependencies
          else npmManifestDependencies.testDevDependencies
        ) ++ (
          if (emitSourceMaps) npmDevDependencies ++ Seq("source-map-loader" -> "0.1.5", "concat-with-sourcemaps" -> "1.0.4")
          else npmDevDependencies
        )

      val packageJson =
        JS.obj(
          "dependencies" -> JS.objStr(dependencies),
          "devDependencies" -> JS.objStr(devDependencies),
          "scripts" -> JS.obj(
            "bundle" -> JS.str(bundleCommand)
          )
        )
      log.debug("Writing 'package.json'")
      val packageJsonFile = targetDir / "package.json"
      IO.write(packageJsonFile, JS.toJson(packageJson))

      val outputFiles =
        webpackEntries.map { case (key, _) => targetDir / s"$key-bundle.js" }

    ConfigFiles(scalajsConfigFile, packageJsonFile, outputFiles)
  }

}