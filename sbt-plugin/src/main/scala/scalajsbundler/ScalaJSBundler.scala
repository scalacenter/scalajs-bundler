package scalajsbundler

import org.scalajs.core.ir.Utils
import org.scalajs.core.tools.javascript.Trees
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{fastOptJS, fullOptJS}
import sbt._
import sbt.Keys._

import JS.syntax._

object ScalaJSBundler extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  override lazy val trigger = allRequirements

  object autoImport {

    val npmDependencies = settingKey[Map[String, String]]("NPM dependencies (libraries that your program uses)")

    val npmDevDependencies = settingKey[Map[String, String]]("NPM dev dependencies (libraries that the build uses)")

    val webpackVersion = settingKey[String]("Version of webpack to use")

    val webpackConfigFile = settingKey[Option[File]]("Configuration file to use with webpack")

    val webpackSourceMap = settingKey[Boolean]("Whether to enable (or not) source-map in webpack")

    val bundle = taskKey[File]("Bundle the output of the fastOptJS task")

    val bundleOpt = taskKey[File]("Bundle the output of the fullOptJS task")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    Seq(
      webpackVersion := "1.13",
      webpackConfigFile := None
    ) ++
    inConfig(Compile)(perConfigSettings) ++
    inConfig(Test)(perConfigSettings)

  private val perConfigSettings: Seq[Def.Setting[_]] =
    Seq(
      npmDependencies := Map.empty,
      npmDevDependencies := Map("webpack" -> webpackVersion.value),
      webpackSourceMap in fullOptJS := false,
      webpackSourceMap in fastOptJS := true,
      bundle := Def.taskDyn(bundleTask(fastOptJS)).value,
      bundleOpt := Def.taskDyn(bundleTask(fullOptJS)).value
    )

  private def bundleTask(stage: TaskKey[Attributed[File]]): Def.Initialize[Task[File]] =
    Def.task {
      val log = streams.value.log
      val targetDir = (crossTarget in stage).value

      val stageOutput = stage.value.data

      // Create a launcher (TODO remove as soon as they disappear from Scala.js)
      val launcherContent = {
        val module = JS.ref("require")(JS.str(stageOutput.absolutePath))
        val mainClassParts =
          mainClass.value.getOrElse(sys.error("No main class detected")).split('.')
        val mainClassRef =
          mainClassParts.foldLeft[Trees.Tree](module) { (tree, part) => tree.bracket(part) }
        (mainClassRef() `.` "main")()
      }
      val launcherFile = targetDir / "launcher.js"
      IO.write(launcherFile, launcherContent.show)

      val bundleFile = targetDir / (stageOutput.name.stripSuffix(".js") ++ "-bundle.js")

      // Create scalajs.webpack.config.js
      val scalajsConfigFile = targetDir / "scalajs.webpack.config.js"
      val scalajsConfigContent =
        JS.ref("module") `.` "exports" := JS.obj(
          "entry" -> JS.str(launcherFile.absolutePath),
          "output" -> JS.obj(
            "path" -> JS.str(targetDir.absolutePath),
            "filename" -> JS.str(bundleFile.name)
          )
        )
      IO.write(scalajsConfigFile, scalajsConfigContent.show)

      // Create a package.json file
      val bundleCommand =
        (webpackConfigFile in stage).value match {
          case Some(configFile) =>
            val configFileCopy = targetDir / configFile.name
            IO.copyFile(configFile, configFileCopy)
            s"webpack --config ${configFileCopy.absolutePath}"
          case None =>
            s"webpack --config ${scalajsConfigFile.absolutePath}"
        }

      def toJsonObject(deps: Map[String, String]): Trees.ObjectConstr =
        JS.obj(deps.mapValues(JS.str).to[Seq]: _*)

      val packageJson =
        JS.obj(
          "dependencies" -> toJsonObject(npmDependencies.value),
          "devDependencies" -> toJsonObject(npmDevDependencies.value),
          "scripts" -> JS.obj(
            "bundle" -> JS.str(bundleCommand)
          )
        )
      IO.write(targetDir / "package.json", JS.toJson(packageJson))

      val process =
        Process("npm update", targetDir) #&& Process("npm run bundle", targetDir)
      process.run(log).exitValue()

      bundleFile
    }

}
