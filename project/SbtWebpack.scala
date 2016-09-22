import org.scalajs.core.ir.Utils
import sbt._
import sbt.Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.{fastOptJS, fullOptJS}

object SbtWebpack extends AutoPlugin {

  object autoImport {

    val npmDependencies = settingKey[Map[String, String]]("NPM dependencies (libraries that your program uses)")

    val npmDevDependencies = settingKey[Map[String, String]]("NPM dev dependencies (libraries that the build uses)")

    val webpackVersion = settingKey[String]("Version of webpack to use")

    val webpackConfigFile = settingKey[Option[File]]("Configuration file to use with Webpack")

    val bundle = taskKey[File]("Bundle the output of the fastOptJS task")

    val bundleOpt = taskKey[File]("Bundle the output of the fullOptJS task")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(perConfigSettings) ++
    inConfig(Test)(perConfigSettings) ++ Seq(
      webpackVersion := "1.13",
      webpackConfigFile := None
    )

  private val perConfigSettings: Seq[Def.Setting[_]] =
    Seq(
      npmDependencies := Map.empty,
      npmDevDependencies := Map("webpack" -> webpackVersion.value),
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

      // Create a package.json file
      def toJsonObject(deps: Map[String, String]): String =
        (
          for ((name, version) <- deps)
          yield s""" "${Utils.escapeJS(name)}": "${Utils.escapeJS(version)}" """
        ).mkString("{", ",", "}")

      val bundleCommand =
        (webpackConfigFile in stage).value match {
          case Some(configFile) =>
            val scalajsConfigFile = targetDir / "scalajs-webpack-config.js"
            val scalajsConfigContent =
              s"""module.exports = {
                |  "entry": "${Utils.escapeJS(launcherFile.absolutePath)}",
                |  "output": {
                |    "path": "${Utils.escapeJS(targetDir.absolutePath)}",
                |    "filename": "${Utils.escapeJS(bundleFile.name)}"
                |  }
                |}""".stripMargin
            IO.write(scalajsConfigFile, scalajsConfigContent)
            val configFileCopy = targetDir / configFile.name
            IO.copyFile(configFile, configFileCopy)
            s"webpack --config ${configFileCopy.absolutePath}"
          case None => s"webpack ${launcherFile.absolutePath} ${bundleFile.absolutePath}"
        }

      val packageJson =
        s"""{
            |  "dependencies": ${toJsonObject(npmDependencies.value)},
            |  "devDependencies": ${toJsonObject(npmDevDependencies.value)},
            |  "scripts": {
            |    "bundle": "${Utils.escapeJS(bundleCommand)}"
            |  }
            |}""".stripMargin
      IO.write(targetDir / "package.json", packageJson)

      val process =
        Process("npm update", targetDir) #&&
          Process("npm run bundle", targetDir)
      process.run(log).exitValue()

      bundleFile
    }

}