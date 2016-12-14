package scalajsbundler.util

import sbt._

object Commands {

  def run(cmd: String, cwd: File, logger: Logger): Unit = {
    val process = Process(cmd, cwd)
    val code = process ! logger
    if (code != 0) {
      sys.error(s"Non-zero exit code: $code")
    }
    ()
  }

}
