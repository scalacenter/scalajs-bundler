package scalajsbundler

import sbt._
import java.io.File

/**
  * Simple wrapper over webpack-dev-server
  */
private [scalajsbundler] class WebpackDevServer {

  private var worker: Option[Worker] = None

  /**
    * @param workDir - path to working directory for webpack-dev-server
    * @param configPath - path to webpack config.
    * @param extraArgs - additional arguments for webpack-dev-server.
    * @param logger - a logger to use for output
    * @param globalLogger - a global logger to use for output even when the task is terminated
    */
  def start(
    workDir: File,
    configPath: File,
    extraArgs: Seq[String],
    logger: Logger,
    globalLogger: Logger,
  ) = this.synchronized {
    stop()
    worker = Some(new Worker(
      workDir,
      configPath,
      extraArgs,
      logger,
      globalLogger
    ))
  }

  def stop() = this.synchronized {
    worker.foreach { w => {
      w.stop()
      worker = None
    }}
  }

  private class Worker(
    workDir: File,
    configPath: File,
    extraArgs: Seq[String],
    logger: Logger,
    globalLogger: Logger,
  ) {
    logger.info("Starting webpack-dev-server");

    val command = Seq(
      "node",
      "node_modules/webpack/bin/webpack",
      "serve",
      "--config",
      configPath.getAbsolutePath
    ) ++ extraArgs

    val process = util.Commands.start(command, workDir, globalLogger)

    def stop() = {
      logger.info("Stopping webpack-dev-server");
      process.destroy()
    }
  }

  override def finalize() = stop()
}
