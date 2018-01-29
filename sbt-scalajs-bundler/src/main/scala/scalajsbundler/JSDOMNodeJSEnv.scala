package scalajsbundler

import java.io.OutputStream

import org.scalajs.core.tools.io.{FileVirtualFile, FileVirtualJSFile, VirtualJSFile}
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.jsenv.{AsyncJSRunner, ComJSRunner, JSRunner}
import org.scalajs.core.ir.Utils.escapeJS
import org.scalajs.jsenv.nodejs.AbstractNodeJSEnv
import sbt._

// HACK Copy of Scala.js’ JSDOMNodeJSEnv. The only change is the ability to pass the directory in which jsdom has been installed
class JSDOMNodeJSEnv(
  jsDomDirectory: File,
  nodejsPath: String = "node",
  addArgs: Seq[String] = Seq.empty,
  addEnv: Map[String, String] = Map.empty
) extends AbstractNodeJSEnv(nodejsPath, addArgs, addEnv, sourceMap = false) {

  protected def vmName: String = "Node.js with JSDOM"

  override def jsRunner(libs: Seq[ResolvedJSDependency],
      code: VirtualJSFile): JSRunner = {
    new DOMNodeRunner(libs, code)
  }

  override def asyncRunner(libs: Seq[ResolvedJSDependency],
      code: VirtualJSFile): AsyncJSRunner = {
    new AsyncDOMNodeRunner(libs, code)
  }

  override def comRunner(libs: Seq[ResolvedJSDependency],
      code: VirtualJSFile): ComJSRunner = {
    new ComDOMNodeRunner(libs, code)
  }

  protected class DOMNodeRunner(libs: Seq[ResolvedJSDependency], code: VirtualJSFile)
      extends ExtRunner(libs, code) with AbstractDOMNodeRunner

  protected class AsyncDOMNodeRunner(libs: Seq[ResolvedJSDependency], code: VirtualJSFile)
      extends AsyncExtRunner(libs, code) with AbstractDOMNodeRunner

  protected class ComDOMNodeRunner(libs: Seq[ResolvedJSDependency], code: VirtualJSFile)
      extends AsyncDOMNodeRunner(libs, code) with NodeComJSRunner

  protected trait AbstractDOMNodeRunner extends AbstractNodeRunner {

    protected def codeWithJSDOMContext(): Seq[VirtualJSFile] = {
      val scriptsJSPaths = getLibJSFiles().map {
        case file: FileVirtualFile => file.path
        case file                  => libCache.materialize(file).getAbsolutePath
      }
      val scriptsStringPath = scriptsJSPaths.map('"' + escapeJS(_) + '"')
      val jsDOMCode = {
        s"""
           |(function () {
           |  var jsdom;
           |  try {
           |    jsdom = require("jsdom/lib/old-api.js"); // jsdom >= 10.x
           |  } catch (e) {
           |    jsdom = require("jsdom"); // jsdom <= 9.x
           |  }
           |  var windowKeys = [];
           |
           |  jsdom.env({
           |    html: "",
           |    virtualConsole: jsdom.createVirtualConsole().sendTo(console),
           |    created: function (error, window) {
           |      if (error == null) {
           |        window["__ScalaJSEnv"] = __ScalaJSEnv;
           |        window["scalajsCom"] = global.scalajsCom;
           |        windowKeys = Object.keys(window);
           |      } else {
           |        console.log(error);
           |      }
           |    },
           |    scripts: [${scriptsStringPath.mkString(", ")}],
           |    onload: function (window) {
           |      jsdom.changeURL(window, "http://localhost");
           |      for (var k in window) {
           |        if (windowKeys.indexOf(k) == -1)
           |          global[k] = window[k];
           |      }
           |
           |      ${code.content}
           |    }
           |  });
           |})();
           |""".stripMargin
      }
      val codeFile = jsDomDirectory / "codeWithJSDOMContext.js"
      IO.write(codeFile, jsDOMCode)
      Seq(FileVirtualJSFile(codeFile))
    }

    override protected def getJSFiles(): Seq[VirtualJSFile] =
      initFiles() ++ customInitFiles() ++ codeWithJSDOMContext()

    /** Libraries are loaded via scripts in Node.js */
    override protected def getLibJSFiles(): Seq[VirtualJSFile] =
      libs.map(_.lib)

    // Send code to Stdin
    override protected def sendVMStdin(out: OutputStream): Unit = {
      /* Do not factor this method out into AbstractNodeRunner or when mixin in
       * the traits it would use AbstractExtRunner.sendVMStdin due to
       * linearization order.
       */
      sendJS(getJSFiles(), out)
    }
  }

}
