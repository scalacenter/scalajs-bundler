package scalajsbundler

import sbt._

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
      callEntryPoint(mainClass, module)
    }
    val launcherFile = targetDir / "launcher.js" // TODO Use different names according to the sjsStages
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
