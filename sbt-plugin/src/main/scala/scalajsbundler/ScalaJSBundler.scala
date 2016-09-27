package scalajsbundler

import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt._

object ScalaJSBundler extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  override lazy val trigger = allRequirements

  object autoImport {

    val npmDependencies = settingKey[Map[String, String]]("NPM dependencies (libraries that your program uses)")

    val npmDevDependencies = settingKey[Map[String, String]]("NPM dev dependencies (libraries that the build uses)")

    val npmUpdate = taskKey[File]("Fetch NPM dependencies")

    val webpackVersion = settingKey[String]("Version of webpack to use")

    val webpackConfigFile = settingKey[Option[File]]("Configuration file to use with webpack")

    val webpackSourceMap = settingKey[Boolean]("Whether to enable (or not) source-map in webpack")

    val bundle = taskKey[File]("Bundle the output of the fastOptJS task")

    val bundleOpt = taskKey[File]("Bundle the output of the fullOptJS task")
  }

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    ScalaJSBundlerInternal.projectSettings

}
