package scalajsbundler

import org.scalajs.sbtplugin.Stage
import sbt._

/**
  * @param file File that contains the launcher
  * @param mainClass JSMain class fully qualified name
  */
case class Launcher(
  file: File,
  mainClass: String
)

object Launcher {

  /**
    * Generates a launcher for the Scala.js output.
    *
    * @param targetDir The npm directory
    * @param sjsOutput Output of Scala.js
    * @param sjsStage Scala.js stage (FastOpt or FullOpt)
    * @param mainClass Main class name
    * @return The written file and the main class name
    */
  // TODO Caching
  def write(
    targetDir: File,
    sjsOutput: Attributed[File],
    sjsStage: Stage,
    mainClass: String
  ): Launcher = {

    val launcherContent = {
      val module = JS.ref("require")(JS.str(sjsOutput.data.absolutePath))
      callEntryPoint(mainClass, module)
    }

    val stagePart =
      sjsStage match {
        case Stage.FastOpt => "fastopt"
        case Stage.FullOpt => "opt"
      }

    val launcherFile = targetDir / s"$stagePart-launcher.js"
    IO.write(launcherFile, launcherContent.show)

    Launcher(launcherFile, mainClass)
  }

  /**
    * @param mainClass Main class name
    * @param module Module exporting the entry point
    * @return A JavaScript program that calls the main method of the main class
    */
  def callEntryPoint(mainClass: String, module: JS): JS = {
    val mainClassRef =
      mainClass
        .split('.')
        .foldLeft(module)((tree, part) => tree.bracket(part))
    mainClassRef.apply().dot("main").apply()
  }

}
