package scalajsbundler.util

import sbt.Logger
import java.io.File
import scala.sys.process.Process
import scala.sys.process.ProcessLogger

object Commands {

  def run(cmd: Seq[String], cwd: File, logger: Logger): Unit = {
    val process = Process(cmd, cwd)
    val code = process ! toProcessLogger(logger)
    if (code != 0) {
      sys.error(s"Non-zero exit code: $code")
    }
    ()
  }

  def start(cmd: Seq[String], cwd: File, logger: Logger): Process =
    Process(cmd, cwd).run(toProcessLogger(logger))

  private def toProcessLogger(logger: Logger): ProcessLogger =
    ProcessLogger(msg => logger.info(msg), msg => logger.error(msg))

}
