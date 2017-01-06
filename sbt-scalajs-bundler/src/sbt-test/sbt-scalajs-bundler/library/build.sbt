name := "library"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

npmDependencies in Compile += "node-uuid" -> "1.4.7"

//#relevant-settings
webpackConfigFile := Some(baseDirectory.value / "webpack.config.js")

import org.scalajs.core.tools.io.{VirtualJSFile, FileVirtualJSFile}

// Use the output of Scala.js as a “launcher”
scalaJSLauncher in (Compile, fullOptJS) := {
  Attributed.blank[VirtualJSFile](FileVirtualJSFile((fullOptJS in Compile).value.data))
}
//#relevant-settings

// Check that a HTML can be loaded (and that its JavaScript can be executed) without errors
InputKey[Unit]("html") := {
  import complete.DefaultParsers._
  val page = (Space ~> StringBasic).parsed
  import com.gargoylesoftware.htmlunit.WebClient
  val client = new WebClient()
  try {
    client.getPage(s"file://${baseDirectory.value.absolutePath}/$page")
  } finally {
    client.close()
  }
}
