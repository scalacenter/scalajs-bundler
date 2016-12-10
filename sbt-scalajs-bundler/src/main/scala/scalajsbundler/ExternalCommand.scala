package scalajsbundler

import sbt._

trait ExternalCommand {

  /**
    * Runs the command `cmd`
    * @param args Command arguments
    * @param workingDir Working directory of the process
    * @param logger Logger
    */
  def run(args: String*)(workingDir: File, logger: Logger): Unit =
    Commands.run((cmd +: args).mkString(" "), workingDir, logger)

  protected def cmd: String

}

object Npm extends ExternalCommand {

  protected lazy val cmd = sys.props("os.name").toLowerCase match {
    case os if os.contains("win") ⇒ "cmd /c npm"
    case _ ⇒ "npm"
  }
}

object Yarn extends ExternalCommand {

  protected lazy val cmd = "yarn"

}

