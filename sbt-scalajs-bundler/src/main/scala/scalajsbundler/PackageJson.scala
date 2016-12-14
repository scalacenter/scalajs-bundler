package scalajsbundler

import sbt._

import scalajsbundler.util.JS

object PackageJson {

  /**
    * Write a package.json file defining the NPM dependencies of the application, plus the ones
    * required to do the bundling.
    *
    * @param log Logger
    * @param targetFile File to write into
    * @param npmDependencies NPM dependencies
    * @param npmDevDependencies NPM devDependencies
    * @param fullClasspath Classpath (used to look for dependencies of Scala.js libraries this project depends on)
    * @param currentConfiguration Current configuration
    * @return The created package.json file (should be `targetDir / "package.json"`)
    */
  def write(
    log: Logger,
    targetFile: File,
    npmDependencies: Seq[(String, String)],
    npmDevDependencies: Seq[(String, String)],
    fullClasspath: Seq[Attributed[File]],
    currentConfiguration: Configuration,
    webpackVersion: String
  ): Unit = {
    val npmManifestDependencies = NpmDependencies.collectFromClasspath(fullClasspath)
    val dependencies =
      npmDependencies ++ (
        if (currentConfiguration == Compile) npmManifestDependencies.compileDependencies
        else npmManifestDependencies.testDependencies
      )
    val devDependencies =
      npmDevDependencies ++ (
        if (currentConfiguration == Compile) npmManifestDependencies.compileDevDependencies
        else npmManifestDependencies.testDevDependencies
      ) ++ Seq(
        "webpack" -> webpackVersion,
        "concat-with-sourcemaps" -> "1.0.4", // Used by the reload workflow
        "source-map-loader" -> "0.1.5" // Used by webpack when emitSourceMaps is enabled
      )

    def filtered(deps: Seq[(String, String)]) =
      deps
        .groupBy { case (name, version) => name }
        .mapValues(_.map(_._2).distinct)
        .foldRight(List.empty[(String, String)]) { case ((name, versions), result) =>
          assert(versions.size == 1, s"Different versions of '$name' are required: $versions")
          (name -> versions.head) :: result
        }

    val packageJson =
      JS.obj(
        "dependencies" -> JS.objStr(filtered(dependencies)),
        "devDependencies" -> JS.objStr(filtered(devDependencies))
      )
    log.debug("Writing 'package.json'")
    IO.write(targetFile, JS.toJson(packageJson))
    ()
  }

}
