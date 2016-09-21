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

      bundle := {
        val log = streams.value.log
        val npmTarget = target.value / "npm"
        val deps = npmDependencies.value
        val fastOptJSOutput = fastOptJS.value.data // TODO support fullopt too

        IO.createDirectory(npmTarget)

        // Copy the output of Scala.js compilation
        val scalaJsModuleFile = npmTarget / fastOptJSOutput.name
        IO.copyFile(fastOptJSOutput, scalaJsModuleFile)

        // Create a launcher
        val mainFqp =
          mainClass.value.getOrElse(throw new IllegalStateException("No main class defined"))
            .split('.')
            .map(p => s"""["${Utils.escapeJS(p)}"]""")
            .mkString
        val launcherContent =
          s"""
            |require("${scalaJsModuleFile.absolutePath}")$mainFqp().main();
          """.stripMargin
        val launcherFile = npmTarget / "launcher.js"
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
        IO.write(npmTarget / "package.json", packageJson)
        val process =
          Process("npm install", npmTarget) #&&
          Process("npm run bundle", npmTarget)
        process.run(log).exitValue()
        npmTarget
      }
    )

}