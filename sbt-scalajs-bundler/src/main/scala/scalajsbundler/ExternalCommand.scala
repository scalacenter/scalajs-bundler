package scalajsbundler

import java.io.File

import sbt._
import scalajsbundler.util.Commands

/**
  * Attempts to smoothen platform-specific differences when invoking commands.
  *
  * @param name Name of the command to run
  */
class ExternalCommand(name: String) {

  /**
    * Runs the command `cmd`
    * @param args Command arguments
    * @param workingDir Working directory of the process
    * @param logger Logger
    */
  def run(args: String*)(workingDir: File, logger: Logger): Unit =
    Commands.run(cmd ++: args, workingDir, logger)

  private val cmd = sys.props("os.name").toLowerCase match {
    case os if os.contains("win") => Seq("cmd", "/c", name)
    case _                        => Seq(name)
  }

}

object Npm extends ExternalCommand("npm")

object Yarn extends ExternalCommand("yarn")

object ExternalCommand {
  private val yarnOptions = List("--non-interactive", "--mutex", "network")
  private def syncYarnLockfile(baseDir: File, installDir: File, logger: Logger)(
      yarnCommand: => Unit): Unit = {
    val sourceLockFile = baseDir / "yarn.lock"
    val targetLockFile = installDir / "yarn.lock"
    if (sourceLockFile.exists()) {
      logger.info("Using lockfile " + sourceLockFile)
      IO.copyFile(sourceLockFile, targetLockFile)
    }

    yarnCommand

    if (targetLockFile.exists()) {
      logger.debug("Wrote lockfile to " + sourceLockFile)
      IO.copyFile(targetLockFile, sourceLockFile)
    }
  }

  /**
    * Locally install NPM packages
    *
    * @param baseDir The (sub-)project directory which contains yarn.lock
    * @param installDir The directory in which to install the packages
    * @param useYarn Whether to use yarn or npm
    * @param logger sbt logger
    * @param npmExtraArgs Additional arguments to pass to npm
    * @param npmPackages Packages to install (e.g. "webpack", "webpack@2.2.1")
    */
  def addPackages(baseDir: File,
                  installDir: File,
                  useYarn: Boolean,
                  logger: Logger,
                  npmExtraArgs: Seq[String],
                  yarnExtraArgs: Seq[String])(npmPackages: String*): Unit =
    if (useYarn) {
      syncYarnLockfile(baseDir, installDir, logger) {
        Yarn.run("add" +: (yarnOptions ++ yarnExtraArgs ++ npmPackages): _*)(
          installDir,
          logger)
      }
    } else {
      Npm.run("install" +: (npmPackages ++ npmExtraArgs): _*)(installDir,
                                                              logger)
    }

  def install(baseDir: File,
              installDir: File,
              useYarn: Boolean,
              logger: Logger,
              npmExtraArgs: Seq[String],
              yarnExtraArgs: Seq[String]): Unit =
    if (useYarn) {
      syncYarnLockfile(baseDir, installDir, logger) {
        Yarn.run("install" +: (yarnOptions ++ yarnExtraArgs): _*)(installDir,
                                                                  logger)
      }
    } else {
      Npm.run("install" +: npmExtraArgs: _*)(installDir, logger)
    }
}
