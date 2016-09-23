import org.scalajs.core.ir.Utils
import sbt._
import sbt.Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.{fastOptJS, fullOptJS}
import org.scalajs.core.tools.javascript.Trees

import scalajsbundler.JS
import scalajsbundler.JS.syntax._

object ScalaJSBundler extends AutoPlugin {

  object autoImport {

    val npmDependencies = settingKey[Map[String, String]]("NPM dependencies (libraries that your program uses)")

    val npmDevDependencies = settingKey[Map[String, String]]("NPM dev dependencies (libraries that the build uses)")

    val webpackVersion = settingKey[String]("Version of Webpack to use")

    val webpackConfigFile = settingKey[Option[File]]("Configuration file to use with Webpack")

    val webpackSourceMap = settingKey[Boolean]("Whether to enable (or not) source-map in Webpack")

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
      val mainFqp =
      mainClass.value.getOrElse(sys.error("No main class detected"))
        .split('.')
        .map(p => s"""["${Utils.escapeJS(p)}"]""")
        .mkString
      val launcherContent =
        s"""
           |require("${Utils.escapeJS(stageOutput.absolutePath)}")$mainFqp().main();
            """.stripMargin
      val launcherFile = targetDir / "launcher.js"
      IO.write(launcherFile, launcherContent)

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