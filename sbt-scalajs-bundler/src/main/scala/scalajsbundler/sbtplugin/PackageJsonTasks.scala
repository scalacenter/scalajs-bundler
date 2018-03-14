package scalajsbundler.sbtplugin

import sbt._

import scalajsbundler.{BundlerFile, PackageJson}
import scalajsbundler.util.{Caching, JSON}

object PackageJsonTasks {

  /**
    * Writes the package.json file that describes the project dependencies
    * @param targetDir Directory in which write the file
    * @param npmDependencies NPM dependencies
    * @param npmDevDependencies NPM devDependencies
    * @param npmResolutions Resolutions to use in case of conflicts
    * @param additionalNpmConfig Additional options to include in 'package.json'
    * @param fullClasspath Classpath
    * @param configuration Current configuration (Compile or Test)
    * @param webpackVersion Webpack version
    * @param webpackDevServerVersion Webpack development server version
    * @return The written package.json file
    */
  def writePackageJson(
    targetDir: File,
    npmDependencies: Seq[(String, String)],
    npmDevDependencies: Seq[(String, String)],
    npmResolutions: Map[String, String],
    additionalNpmConfig: Map[String, JSON],
    fullClasspath: Seq[Attributed[File]],
    configuration: Configuration,
    webpackVersion: String,
    webpackDevServerVersion: String,
    streams: Keys.TaskStreams
  ): BundlerFile.PackageJson = {

    val hash = Seq(
      configuration.name,
      npmDependencies.toString,
      npmDevDependencies.toString,
      npmResolutions.toString,
      fullClasspath.map(_.data.name).toString,
      webpackVersion,
      webpackDevServerVersion
    ).mkString(",")

    val packageJsonFile = targetDir / "package.json"

    Caching.cached(
      packageJsonFile,
      hash,
      streams.cacheDirectory / s"scalajsbundler-package-json-${if (configuration == Compile) "main" else "test"}"
    ) { () =>
      PackageJson.write(
        streams.log,
        packageJsonFile,
        npmDependencies,
        npmDevDependencies,
        npmResolutions,
        additionalNpmConfig,
        fullClasspath,
        configuration,
        webpackVersion,
        webpackDevServerVersion
      )
      ()
    }

    BundlerFile.PackageJson(packageJsonFile)
  }

}
