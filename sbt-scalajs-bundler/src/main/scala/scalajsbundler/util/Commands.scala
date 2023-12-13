package scalajsbundler.util

import sbt.Logger
import java.io.{File, InputStream}

import scala.sys.process.Process
import scala.sys.process.BasicIO
import scala.sys.process.ProcessLogger
import scala.util.Try

object Commands {

  def run[A](cmd: Seq[String], cwd: File, logger: Logger, outputProcess: InputStream => Try[A]): Either[String, Option[Try[A]]] = {
    val toErrorLog = (is: InputStream) => {
      scala.io.Source.fromInputStream(is).getLines.foreach(msg => logger.error(msg))
      is.close()
    }

    // Unfortunately a var is the only way to capture the result
    var result: Option[Try[A]] = None
    def outputCapture(o: InputStream): Unit = {
      result = Some(outputProcess(o))
      o.close()
      ()
    }

    logger.debug(s"Command: ${cmd.mkString(" ")}")
    val process = Process(cmd, cwd)
    val processIO = BasicIO.standard(false).withOutput(outputCapture).withError(toErrorLog)
    val code: Int = process.run(processIO).exitValue()
    if (code != 0) {
      Left(s"Non-zero exit code: $code")
    } else {
      Right(result)
    }
  }

  def run(cmd: Seq[String], cwd: File, logger: Logger): Unit = {
    val toInfoLog = (is: InputStream) => Try(scala.io.Source.fromInputStream(is).getLines.foreach(msg => logger.info(msg)))
    run(cmd, cwd, logger, toInfoLog).fold(sys.error, _ => ())
  }

  def start(cmd: Seq[String], cwd: File, logger: Logger): Process =
    Process(cmd, cwd).run(toProcessLogger(logger))

  private def toProcessLogger(logger: Logger): ProcessLogger =
    ProcessLogger(msg => logger.info(msg), msg => logger.error(msg))

}
