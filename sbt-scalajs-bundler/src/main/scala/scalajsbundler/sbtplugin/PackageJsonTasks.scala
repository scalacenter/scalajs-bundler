package scalajsbundler.sbtplugin

import sbt._

import scalajsbundler.PackageJson
import scalajsbundler.util.{Caching, JS}

object PackageJsonTasks {

  /**
    * Writes the package.json file that describes the project dependencies
    * @param targetDir Directory in which write the file
    * @param npmDependencies NPM dependencies
    * @param npmDevDependencies NPM devDependencies
    * @param npmResolutions Resolutions to use in case of conflicts
    * @param npmConfig Additional options to include in 'package.json'
    * @param fullClasspath Classpath
    * @param configuration Current configuration (Compile or Test)
    * @param webpackVersion Webpack version
    * @return The written package.json file
    */
  def writePackageJson(
    targetDir: File,
    npmDependencies: Seq[(String, String)],
    npmDevDependencies: Seq[(String, String)],
    npmResolutions: Map[String, String],
    npmConfig: Map[String, JS],
    fullClasspath: Seq[Attributed[File]],
    configuration: Configuration,
    webpackVersion: String,
    streams: Keys.TaskStreams
  ): File = {

    val hash = Seq(
      configuration.name,
      npmDependencies.toString,
      npmDevDependencies.toString,
      npmResolutions.toString,
      fullClasspath.map(_.data.name).toString,
      webpackVersion
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
        npmConfig,
        fullClasspath,
        configuration,
        webpackVersion
      )
      ()
    }

    packageJsonFile
  }

}
