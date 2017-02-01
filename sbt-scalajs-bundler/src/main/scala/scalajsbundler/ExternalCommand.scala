package scalajsbundler

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

  val cmd =
    sys.props("os.name").toLowerCase match {
      case os if os.contains("win") => Seq("cmd", "/c", name)
      case _ => Seq(name)
    }

}

object Npm extends ExternalCommand("npm")

object Yarn extends ExternalCommand("yarn")

object ExternalCommand {

  /**
    * Locally install NPM packages
    *
    * @param installDir The directory in which to install the packages
    * @param useYarn Whether to use yarn or npm
    * @param logger sbt logger
    * @param npmPackages Packages to install (e.g. "webpack", "webpack@2.2.1")
    */
  def install(installDir: File, useYarn: Boolean, logger: Logger)(npmPackages: String*): Unit =
    if (useYarn) {
      Yarn.run("add" +: npmPackages: _*)(installDir, logger)
    } else {
      Npm.run("install" +: npmPackages: _*)(installDir, logger)
    }

}