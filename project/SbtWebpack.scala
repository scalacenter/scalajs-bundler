import org.scalajs.core.ir.Utils
import sbt._
import sbt.Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.fastOptJS

object SbtWebpack extends AutoPlugin {

  object autoImport {
    val npmDependencies = settingKey[Map[String, String]]("NPM dependencies")

    val bundle = taskKey[File]("Bundle the application")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(perConfigSettings) ++
    inConfig(Test)(perConfigSettings)

  private val perConfigSettings: Seq[Def.Setting[_]] =
    Seq(
      npmDependencies := Map.empty,

      bundle := Def.taskDyn {
        val stage = fastOptJS // TODO support fullopt too
        Def.task {
          val log = streams.value.log
          val targetDir = (crossTarget in stage).value
          val deps = npmDependencies.value

          val stageOutput = stage.value.data

          // Create a launcher (TODO remove as soon as they will disappear from Scala.js)
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

          // Create a package.json file
          val depsJson =
            (for ((name, version) <- deps) yield s""" "$name": "$version" """)
              .mkString("{", ",", "}")
          val packageJson =
            s"""{
                |  "dependencies": $depsJson,
                |  "devDependencies": { "webpack": "1.13" },
                |  "scripts": {
                |    "bundle": "webpack ${launcherFile.absolutePath} app.js"
                |  }
                |}""".stripMargin
          IO.write(targetDir / "package.json", packageJson)
          val process =
            Process("npm update", targetDir) #&&
            Process("npm run bundle", targetDir)
          process.run(log).exitValue()
          targetDir
        }
      }.value
    )

}