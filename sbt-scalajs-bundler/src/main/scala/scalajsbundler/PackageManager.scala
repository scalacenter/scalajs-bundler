package scalajsbundler

import java.io.File

import sbt._
import scalajsbundler.util.Commands
import scalajsbundler.util.JSON

trait PackageManager {

  val name: String

  /**
    * Runs the command `cmd`
    * @param args Command arguments
    * @param workingDir Working directory of the process
    * @param logger Logger
    */
  def run(args: String*)(workingDir: File, logger: Logger): Unit

  def install(baseDir: File, installDir: File, logger: Logger): Unit

  val packageJsonContents: Map[String, JSON]
}

object PackageManager {

  abstract class ExternalProcess(
    val name: String,
    val installCommand: String,
    val installArgs: Seq[String]
  ) extends PackageManager {

    def run(args: String*)(workingDir: File, logger: Logger): Unit =
      Commands.run(cmd ++: args, workingDir, logger)

    private val cmd = sys.props("os.name").toLowerCase match {
      case os if os.contains("win") => Seq("cmd", "/c", name)
      case _                        => Seq(name)
    }

    def install(baseDir: File, installDir: File, logger: Logger): Unit = {
      this match {
        case lfs: LockFileSupport =>
          lfs.lockFileRead(baseDir, installDir, logger)
        case _ =>
          ()
      }

      run(installCommand +: installArgs: _*)(installDir, logger)

      this match {
        case lfs: LockFileSupport =>
          lfs.lockFileWrite(baseDir, installDir, logger)
        case _ =>
          ()
      }
    }
  }

  trait AddPackagesSupport { this: PackageManager =>

    val addPackagesCommand: String
    val addPackagesArgs: Seq[String]

    /**
      * Locally install NPM packages
      *
      * @param baseDir The (sub-)project directory which contains yarn.lock
      * @param installDir The directory in which to install the packages
      * @param logger sbt logger
      * @param npmPackages Packages to install (e.g. "webpack", "webpack@2.2.1")
      */
    def addPackages(baseDir: File,
                    installDir: File,
                    logger: Logger,
                   )(npmPackages: String*): Unit = {
      this match {
        case lfs: LockFileSupport  =>
          lfs.lockFileRead(baseDir, installDir, logger)
        case _ =>
          ()
      }

      run(addPackagesCommand +: (addPackagesArgs ++ npmPackages): _*)(installDir, logger)

      this match {
        case lfs: LockFileSupport  =>
          lfs.lockFileWrite(baseDir, installDir, logger)
        case _ =>
          ()
      }
    }
  }

  trait LockFileSupport {
    val lockFileName: String

    def lockFileRead(
                      baseDir: File,
                      installDir: File,
                      logger: Logger
                    ): Unit = {
      val sourceLockFile = baseDir / lockFileName
      val targetLockFile = installDir / lockFileName

      if (sourceLockFile.exists()) {
        logger.info("Using lockfile " + sourceLockFile)
        IO.copyFile(sourceLockFile, targetLockFile)
      }
    }

    def lockFileWrite(
                       baseDir: File,
                       installDir: File,
                       logger: Logger
                     ): Unit = {
      val sourceLockFile = baseDir / lockFileName
      val targetLockFile = installDir / lockFileName

      if (targetLockFile.exists()) {
        logger.debug("Wrote lockfile to " + sourceLockFile)
        IO.copyFile(targetLockFile, sourceLockFile)
      }
    }
  }

  final class Npm private (
    override val name: String,
    val lockFileName: String,
    override val installCommand: String,
    override val installArgs: Seq[String],
    val addPackagesCommand: String,
    val addPackagesArgs: Seq[String],
  ) extends ExternalProcess(name, installCommand, installArgs)
    with LockFileSupport
    with AddPackagesSupport {

    override val packageJsonContents: Map[String, JSON] = Map.empty

    private def this() = {
      this(
        name = "npm",
        lockFileName = "package-lock.json",
        installCommand = "install",
        installArgs = Seq.empty,
        addPackagesCommand = "install",
        addPackagesArgs = Seq.empty
      )
    }

    def withName(name: String): Npm = copy(name = name)

    def withLockFileName(lockFileName: String): Npm = copy(lockFileName = lockFileName)

    def withInstallCommand(installCommand: String): Npm = copy(installCommand = installCommand)

    def withInstallArgs(installArgs: Seq[String]): Npm = copy(installArgs = installArgs)

    def withAddPackagesCommand(addPackagesCommand: String): Npm = copy(addPackagesCommand = addPackagesCommand)

    def withAddPackagesArgs(addPackagesArgs: Seq[String]): Npm = copy(addPackagesArgs = addPackagesArgs)

    private def copy(
      name: String = name,
      lockFileName: String = lockFileName,
      installCommand: String = installCommand,
      installArgs: Seq[String] = installArgs,
      addPackagesCommand: String = addPackagesCommand,
      addPackagesArgs: Seq[String] = addPackagesArgs
    ) = {
      new Npm(
        name,
        lockFileName,
        installCommand,
        installArgs,
        addPackagesCommand,
        addPackagesArgs
      )
    }
  }
  object Npm {
    def apply() = new Npm()
  }

  final class Yarn private (
    override val name: String,
    val version: Option[String],
    val lockFileName: String,
    override val installCommand: String,
    override val installArgs: Seq[String],
    val addPackagesCommand: String,
    val addPackagesArgs: Seq[String],
  ) extends ExternalProcess(name, installCommand, installArgs)
    with LockFileSupport
    with AddPackagesSupport {

    override val packageJsonContents: Map[String, JSON] =
      version.map(v => Map("packageManager" -> JSON.str(s"$name@$v"))).getOrElse(Map.empty)

    private def this() = {
      this(
        name = "yarn",
        version = None,
        lockFileName = "yarn.lock",
        installCommand = "install",
        installArgs = Seq.empty,
        addPackagesCommand = "add",
        addPackagesArgs = Seq.empty
      )
    }

    def withName(name: String): Yarn = copy(name = name)

    def withVersion(version: Option[String]): Yarn = copy(version = version)

    def withLockFileName(lockFileName: String): Yarn = copy(lockFileName = lockFileName)

    def withInstallCommand(installCommand: String): Yarn = copy(installCommand = installCommand)

    def withInstallArgs(installArgs: Seq[String]): Yarn = copy(installArgs = installArgs)

    def withAddPackagesCommand(addPackagesCommand: String): Yarn = copy(addPackagesCommand = addPackagesCommand)

    def withAddPackagesArgs(addPackagesArgs: Seq[String]): Yarn = copy(addPackagesArgs = addPackagesArgs)

    private def copy(
      name: String = name,
      version: Option[String] = version,
      lockFileName: String = lockFileName,
      installCommand: String = installCommand,
      installArgs: Seq[String] = installArgs,
      addPackagesCommand: String = addPackagesCommand,
      addPackagesArgs: Seq[String] = addPackagesArgs
    ) = {
      new Yarn(
        name,
        version,
        lockFileName,
        installCommand,
        installArgs,
        addPackagesCommand,
        addPackagesArgs
      )
    }
  }
  object Yarn {
    val DefaultArgs: Seq[String] = Seq("--non-interactive", "--mutex", "network")

    def apply() = new Yarn()
  }
}
