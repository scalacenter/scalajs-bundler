package scalajsbundler

import sbt._

object Commands {

  private val npm = sys.props("os.name").toLowerCase match {
    case os if os.contains("win") ⇒ "cmd /c npm"
    case _ ⇒ "npm"
  }

  /**
    * Runs the `npm update` command
    * @param cwd Working directory of the process
    * @param log Logger
    */
  def npmUpdate(cwd: File, log: Logger): Unit = {
    run(s"$npm update", cwd, log)
  }

  def run(cmd: String, cwd: File, logger: Logger): Unit = {
    val process = Process(cmd, cwd)
    val code = process ! logger
    if (code != 0) {
      sys.error(s"Non-zero exit code: $code")
    }
    ()
  }

}
