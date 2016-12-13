package scalajsbundler

import sbt._

object PackageJsonTasks {

  /**
    * Writes the package.json file that describes the project dependencies
    * @param targetDir Directory in which write the file
    * @param npmDependencies NPM dependencies
    * @param npmDevDependencies NPM devDependencies
    * @param fullClasspath Classpath
    * @param configuration Current configuration (Compile or Test)
    * @param webpackVersion Webpack version
    * @return The written package.json file
    */
  def writePackageJson(
    targetDir: File,
    npmDependencies: Seq[(String, String)],
    npmDevDependencies: Seq[(String, String)],
    fullClasspath: Seq[Attributed[File]],
    configuration: Configuration,
    webpackVersion: String,
    streams: Keys.TaskStreams
  ): File = {

    val packageJsonFile = targetDir / "package.json"

    Caching.cached(
      packageJsonFile,
      configuration.name,
      streams.cacheDirectory / s"scalajsbundler-package-json-${if (configuration == Compile) "main" else "test"}"
    ) { () =>
      PackageJson.write(
        streams.log,
        packageJsonFile,
        npmDependencies,
        npmDevDependencies,
        fullClasspath,
        configuration,
        webpackVersion
      )
      ()
    }

    packageJsonFile
  }

}
