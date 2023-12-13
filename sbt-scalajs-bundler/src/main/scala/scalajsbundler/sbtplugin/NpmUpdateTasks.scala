package scalajsbundler.sbtplugin

import java.nio.file.Path

import sbt._
import scalajsbundler.PackageManager

object NpmUpdateTasks {

  /**
    * Runs either `npm install` or `yarn install` and installs JavaScript resources as node packages.
    *
    * @param targetDir npm directory
    * @param packageJsonFile Json file containing NPM dependencies
    * @param useYarn Whether to use yarn or npm
    * @param jsResources A sequence of JavaScript resources
    * @param streams A sbt TaskStream
    * @param npmExtraArgs Additional arguments to pass to npm
    * @param yarnExtraArgs Additional arguments to pass to yarn
    * @return The written npm directory
    */
  @deprecated("Use overload with packageManager instead.")
  def npmUpdate(baseDir: File,
                targetDir: File,
                packageJsonFile: File,
                useYarn: Boolean,
                jsResources: Seq[(String, Path)],
                streams: Keys.TaskStreams,
                npmExtraArgs: Seq[String],
                yarnExtraArgs: Seq[String]): File = {
    npmUpdate(
      baseDir,
      targetDir,
      packageJsonFile,
      jsResources,
      streams,
      if (useYarn){
        PackageManager.Yarn().withInstallArgs(yarnExtraArgs).withAddPackagesArgs(yarnExtraArgs)
      } else {
        PackageManager.Npm().withInstallArgs(npmExtraArgs).withAddPackagesArgs(npmExtraArgs)
      }
    )
  }

  /**
    * Uses package manager to install JavaScript resources as node packages.
    *
    * @param targetDir npm directory
    * @param packageJsonFile Json file containing NPM dependencies
    * @param jsResources A sequence of JavaScript resources
    * @param streams A sbt TaskStream
    * @param packageManager package manager
    * @return The written npm directory
    */
  def npmUpdate(baseDir: File,
                targetDir: File,
                packageJsonFile: File,
                jsResources: Seq[(String, Path)],
                streams: Keys.TaskStreams,
                packageManager: PackageManager): File = {
    val dir = npmInstallDependencies(baseDir, targetDir, packageJsonFile, streams, packageManager)
    npmInstallJSResources(targetDir, jsResources, Seq.empty, streams)
    dir
  }

  /**
    * Runs either `npm install` or `yarn install`.
    *
    * @param targetDir npm directory
    * @param packageJsonFile Json file containing NPM dependencies
    * @param useYarn Whether to use yarn or npm
    * @param streams A sbt TaskStream
    * @param npmExtraArgs Additional arguments to pass to npm
    * @param yarnExtraArgs Additional arguments to pass to yarn
    * @return The written npm directory
    */
  @deprecated("Use overload with packageManager instead.")
  def npmInstallDependencies(baseDir: File,
                             targetDir: File,
                             packageJsonFile: File,
                             useYarn: Boolean,
                             streams: Keys.TaskStreams,
                             npmExtraArgs: Seq[String],
                             yarnExtraArgs: Seq[String]): File = {
    npmInstallDependencies(
      baseDir,
      targetDir,
      packageJsonFile,
      streams,
      if (useYarn){
        PackageManager.Yarn().withInstallArgs(yarnExtraArgs).withAddPackagesArgs(yarnExtraArgs)
      } else {
        PackageManager.Npm().withInstallArgs(npmExtraArgs).withAddPackagesArgs(npmExtraArgs)
      }
    )
  }

  /**
    * Runs install command of package manager.
    *
    * @param targetDir npm directory
    * @param packageJsonFile Json file containing NPM dependencies
    * @param streams A sbt TaskStream
    * @param packageManager package manager
    * @return The written npm directory
    */
  def npmInstallDependencies(baseDir: File,
                             targetDir: File,
                             packageJsonFile: File,
                             streams: Keys.TaskStreams,
                             packageManager: PackageManager): File = {
    val log = streams.log
    val cachedActionFunction =
      FileFunction.cached(
        streams.cacheDirectory / "scalajsbundler-npm-install",
        inStyle = FilesInfo.hash
      ) { _ =>
        log.info("Updating NPM dependencies")
        packageManager.install(baseDir, targetDir, log)
        Set.empty
      }
    cachedActionFunction(Set(packageJsonFile))
    targetDir
  }

  private object PathWithFile {
    def unapply(path: Path): Option[File] = {
      try {
        Some(path.toFile())
      } catch {
        case _: UnsupportedOperationException => None
      }
    }
  }

  /**
    * Installs JavaScript resources as node packages.
    *
    * @param targetDir npm directory
    * @param jsResources The JavaScript resources
    * @param streams A sbt TaskStream
    * @return The paths to the node packages
    */
  def npmInstallJSResources(targetDir: File,
                            jsResources: Seq[(String, Path)],
                            jsSourceDirectories: Seq[File],
                            streams: Keys.TaskStreams): Seq[File] = {
    val jsFileResources =   jsResources.collect {
      case (relativePath, PathWithFile(jsfile)) => jsfile -> relativePath
    }.toSet ++ jsSourceDirectories.flatMap { f =>
      if (f.isDirectory)
        sbt.Path.allSubpaths(f).filterNot(_._1.isDirectory)
      else Seq.empty
    }.toSet

    val cachedActionFunction = FileFunction.cached(
        streams.cacheDirectory / "scalajsbundler-npm-install-resources",
        inStyle = FilesInfo.hash
      ) { _ =>
      jsFileResources.map { case (file, relativePath) =>
        val resourcePath = targetDir / relativePath
        IO.copyFile(file, resourcePath)
        resourcePath
        }
      }
    val files = jsFileResources.map { case (rFile, _) => rFile }
    cachedActionFunction(files).toSeq
  }
}
