package scalajsbundler

import sbt._

import org.scalajs.core.tools.javascript.Trees

import JS.syntax._

/**
  * @param file File that contains the launcher
  * @param mainClass JSMain class fully qualified name
  */
case class Launcher(
  file: File,
  mainClass: String
)

// TODO Remove when launchers are not anymore necessary
object Launcher {

  def write(
    targetDir: File,
    sjsStageOutput: File,
    mainClass: String
  ): Launcher = {

    val launcherContent = {
      val module = JS.ref("require")(JS.str(sjsStageOutput.absolutePath))
      val mainClassRef =
        mainClass
          .split('.')
          .foldLeft[Trees.Tree](module) { (tree, part) => tree.bracket(part) }
      (mainClassRef() `.` "main")()
    }
    val launcherFile = targetDir / "launcher.js" // TODO Use different names according to the sjsStages
    IO.write(launcherFile, launcherContent.show)

    Launcher(launcherFile, mainClass)
  }

}
