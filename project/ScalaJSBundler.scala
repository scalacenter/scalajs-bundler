import org.scalajs.core.ir.Utils
import sbt._
import sbt.Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.{fastOptJS, fullOptJS}
import org.scalajs.core.tools.javascript.{Trees => JS}
import org.scalajs.core.ir.Position

object ScalaJSBundler extends AutoPlugin {

  implicit val scalajsPosition: Position = Position.NoPosition

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
        JS.Assign(
          JS.DotSelect(JS.VarRef(JS.Ident("module")), JS.Ident("exports")),
          JS.ObjectConstr(List(
            JS.StringLiteral("entry") -> JS.StringLiteral(launcherFile.absolutePath),
            JS.StringLiteral("output") -> JS.ObjectConstr(List(
              JS.StringLiteral("path") -> JS.StringLiteral(targetDir.absolutePath),
              JS.StringLiteral("filename") -> JS.StringLiteral(bundleFile.name)
            ))
          ))
        )
      IO.write(scalajsConfigFile, scalajsConfigContent.show)

      // Create a package.json file
      def toJsonObject(deps: Map[String, String]): JS.ObjectConstr =
        JS.ObjectConstr(
          deps.to[List].map { case (k, v) => (JS.StringLiteral(k), JS.StringLiteral(v)) }
        )

      val bundleCommand =
        (webpackConfigFile in stage).value match {
          case Some(configFile) =>
            val configFileCopy = targetDir / configFile.name
            IO.copyFile(configFile, configFileCopy)
            s"webpack --config ${configFileCopy.absolutePath}"
          case None =>
            s"webpack --config ${scalajsConfigFile.absolutePath}"
        }

      val packageJson =
        JS.ObjectConstr(List(
          JS.StringLiteral("dependencies") -> toJsonObject(npmDependencies.value),
          JS.StringLiteral("devDependencies") -> toJsonObject(npmDevDependencies.value),
          JS.StringLiteral("scripts") -> JS.ObjectConstr(List(
            JS.StringLiteral("bundle") -> JS.StringLiteral(bundleCommand)
          ))
        ))
      IO.write(targetDir / "package.json", scalajsbundler.JS.toJson(packageJson))

      val process =
        Process("npm update", targetDir) #&& Process("npm run bundle", targetDir)
      process.run(log).exitValue()

      bundleFile
    }

}