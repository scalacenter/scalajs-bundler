package scalajsbundler

import scala.annotation.tailrec

import scala.collection.immutable
import scala.util.control.NonFatal

import sbt._

import java.io._
import java.nio.file.{Files, StandardCopyOption}
import java.net.URI

import org.scalajs.io._
import org.scalajs.io.JSUtils.escapeJS

import org.scalajs.jsenv._
import org.scalajs.jsenv.nodejs._

// HACK Copy of Scala.js’ JSDOMNodeJSEnv. The only change is the ability to pass the directory in which jsdom has been installed
class JSDOMNodeJSEnv(config: JSDOMNodeJSEnv.Config) extends JSEnv {

  val name: String = "Node.js with JSDOM"

  def start(input: Input, runConfig: RunConfig): JSRun = {
    JSDOMNodeJSEnv.validator.validate(runConfig)
    val scripts = validateInput(input)
    try {
      internalStart(codeWithJSDOMContext(scripts), runConfig)
    } catch {
      case NonFatal(t) =>
        JSRun.failed(t)
    }
  }

  def startWithCom(input: Input, runConfig: RunConfig,
    onMessage: String => Unit): JSComRun = {
    JSDOMNodeJSEnv.validator.validate(runConfig)
    val scripts = validateInput(input)
    ComRun.start(runConfig, onMessage) { comLoader =>
      internalStart(comLoader :: codeWithJSDOMContext(scripts), runConfig)
    }
  }

  private def validateInput(input: Input): List[VirtualBinaryFile] = {
    input match {
      case Input.ScriptsToLoad(scripts) =>
        scripts
      case _ =>
        throw new UnsupportedInputException(input)
    }
  }

  private def internalStart(files: List[VirtualBinaryFile],
    runConfig: RunConfig): JSRun = {
    val command = config.executable :: config.args
    val externalConfig = ExternalJSRun.Config()
      .withEnv(env)
      .withRunConfig(runConfig)
    ExternalJSRun.start(command, externalConfig)(JSDOMNodeJSEnv.write(files))
  }

  private def env: Map[String, String] =
    Map("NODE_MODULE_CONTEXTS" -> "0") ++ config.env

  private def codeWithJSDOMContext(
    scripts: List[VirtualBinaryFile]): List[VirtualBinaryFile] = {

    val scriptsURIs = scripts.map(JSDOMNodeJSEnv.materialize(_))
    val scriptsURIsAsJSStrings =
      scriptsURIs.map(uri => '"' + escapeJS(uri.toASCIIString) + '"')
    val jsDOMCode = {
      s"""
         |(function () {
         |  var jsdom;
         |  try {
         |    jsdom = require("jsdom/lib/old-api.js"); // jsdom >= 10.x
         |  } catch (e) {
         |    jsdom = require("jsdom"); // jsdom <= 9.x
         |  }
         |
         |  var virtualConsole = jsdom.createVirtualConsole()
         |    .sendTo(console, { omitJsdomErrors: true });
         |  virtualConsole.on("jsdomError", function (error) {
         |    /* This inelegant if + console.error is the only way I found
         |     * to make sure the stack trace of the original error is
         |     * printed out.
         |     */
         |    if (error.detail && error.detail.stack)
         |      console.error(error.detail.stack);
         |
         |    // Throw the error anew to make sure the whole execution fails
         |    throw error;
         |  });
         |
         |  jsdom.env({
         |    html: "",
         |    url: "http://localhost/",
         |    virtualConsole: virtualConsole,
         |    created: function (error, window) {
         |      if (error == null) {
         |        window["scalajsCom"] = global.scalajsCom;
         |      } else {
         |        throw error;
         |      }
         |    },
         |    scripts: [${scriptsURIsAsJSStrings.mkString(", ")}]
         |  });
         |})();
         |""".stripMargin
    }

    val codeFile = config.jsDomDirectory / "codeWithJSDOMContext.js"
    IO.write(codeFile, jsDOMCode)
    List(new FileVirtualBinaryFile(codeFile))
  }
}

object JSDOMNodeJSEnv {
  private lazy val validator = ExternalJSRun.supports(RunConfig.Validator())

  // Copied from NodeJSEnv.scala upstream
  private def write(files: List[VirtualBinaryFile])(out: OutputStream): Unit = {
    val p = new PrintStream(out, false, "UTF8")
    try {
      files.foreach {
        case file: FileVirtualBinaryFile =>
          val fname = file.file.getAbsolutePath
          p.println(s"""require("${escapeJS(fname)}");""")
        case f =>
          val in = f.inputStream
          try {
            val buf = new Array[Byte](4096)

            @tailrec
            def loop(): Unit = {
              val read = in.read(buf)
              if (read != -1) {
                p.write(buf, 0, read)
                loop()
              }
            }

            loop()
          } finally {
            in.close()
          }

          p.println()
      }
    } finally {
      p.close()
    }
  }

  // tmpSuffixRE and tmpFile copied from HTMLRunnerBuilder.scala in Scala.js

  private val tmpSuffixRE = """[a-zA-Z0-9-_.]*$""".r

  private def tmpFile(path: String, in: InputStream): URI = {
    try {
      /* - createTempFile requires a prefix of at least 3 chars
       * - we use a safe part of the path as suffix so the extension stays (some
       *   browsers need that) and there is a clue which file it came from.
       */
      val suffix = tmpSuffixRE.findFirstIn(path).orNull

      val f = File.createTempFile("tmp-", suffix)
      f.deleteOnExit()
      Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING)
      f.toURI()
    } finally {
      in.close()
    }
  }

  private def materialize(file: VirtualBinaryFile): URI = {
    file match {
      case file: FileVirtualFile => file.file.toURI
      case file                  => tmpFile(file.path, file.inputStream)
    }
  }

  final class Config private (
    val jsDomDirectory: File,
    val executable: String,
    val args: List[String],
    val env: Map[String, String]
  ) {
    private def this(jsDomDirectory: File) = {
      this(
        jsDomDirectory,
        executable = "node",
        args = Nil,
        env = Map.empty
      )
    }

    def withExecutable(executable: String): Config =
      copy(executable = executable)

    def withArgs(args: List[String]): Config =
      copy(args = args)

    def withEnv(env: Map[String, String]): Config =
      copy(env = env)

    private def copy(
      executable: String = executable,
      args: List[String] = args,
      env: Map[String, String] = env
    ): Config = {
      new Config(jsDomDirectory, executable, args, env)
    }
  }

  object Config {
    /** Returns a default configuration for a [[JSDOMNodeJSEnv]].
      *
      *  The defaults are:
      *
      *  - `executable`: `"node"`
      *  - `args`: `Nil`
      *  - `env`: `Map.empty`
      */
    def apply(jsDomDirectory: File): Config = new Config(jsDomDirectory)
  }
}
