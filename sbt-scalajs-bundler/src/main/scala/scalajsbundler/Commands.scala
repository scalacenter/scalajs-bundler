package scalajsbundler

import sbt._

object Commands {

  /**
    * Runs the `npm update` command
    * @param cwd Working directory of the process
    * @param log Logger
    */
  def npmUpdate(cwd: File, log: Logger): Unit = {
    log.info("Updating NPM dependencies")
    run("npm update", cwd, log)
  }

  /**
    * Runs the `npm run bundle` command
    * @param cwd Working directory of the process
    * @param log Logger
    */
  def bundle(cwd: File, log: Logger): Unit = {
    log.info("Bundling the application with its NPM dependencies")
    run("npm run bundle", cwd, log)
  }

  def run(cmd: String, cwd: File, errorLogger: Logger): Unit = {
    val process = Process(cmd, cwd)
    process !! errorLogger // FIXME Also redirect the standard output of the process to the logger
    ()
  }

}
