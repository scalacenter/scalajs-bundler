package scalajsbundler

import sbt._

object Npm {

  /**
    * Runs the `npm` command
    * @param args Command arguments
    * @param workingDir Working directory of the process
    * @param logger Logger
    */
  def run(args: String*)(workingDir: File, logger: Logger): Unit =
    Commands.run((npm +: args).mkString(" "), workingDir, logger)

  private val npm = sys.props("os.name").toLowerCase match {
    case os if os.contains("win") ⇒ "cmd /c npm"
    case _ ⇒ "npm"
  }

}
