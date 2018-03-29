package scalajsbundler.util

import sbt.Logger
import java.io.{InputStream, File}
import scala.sys.process.Process
import scala.sys.process.BasicIO
import scala.sys.process.ProcessLogger

object Commands {

  def run(cmd: Seq[String], cwd: File, logger: Logger, ouputProcess: InputStream => Unit): Unit = {
    val toErrorLog = (is: InputStream) => scala.io.Source.fromInputStream(is).getLines.foreach(msg => logger.error(msg))

    logger.debug(s"Command: ${cmd.mkString(" ")}")
    val process = Process(cmd, cwd)
    val processIO = BasicIO.standard(false).withOutput(ouputProcess).withError(toErrorLog)
    val code: Int = process.run(processIO).exitValue()
    if (code != 0) {
      sys.error(s"Non-zero exit code: $code")
    }
    ()
  }

  def run(cmd: Seq[String], cwd: File, logger: Logger): Unit = {
    val toInfoLog = (is: InputStream) => scala.io.Source.fromInputStream(is).getLines.foreach(msg => logger.error(msg))
    run(cmd, cwd, logger, toInfoLog)
  }

  def start(cmd: Seq[String], cwd: File, logger: Logger): Process =
    Process(cmd, cwd).run(toProcessLogger(logger))

  private def toProcessLogger(logger: Logger): ProcessLogger =
    ProcessLogger(msg => logger.info(msg), msg => logger.error(msg))

}
