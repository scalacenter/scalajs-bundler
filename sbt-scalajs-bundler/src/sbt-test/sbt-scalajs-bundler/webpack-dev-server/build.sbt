import com.gargoylesoftware.htmlunit.WebClient

name := "webpack-dev-server"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

// Use a custom config file
webpackConfigFile := Some(baseDirectory.value / "webpack.config.js")

(npmDevDependencies in Compile) += ("html-webpack-plugin" -> "2.24.1")

webpackDevServerPort := 7357

// (Used by tests only) checks that a HTML can be loaded (and that its JavaScript can be executed) without errors
InputKey[Unit]("html") := {
  import complete.DefaultParsers._
  val page = (Space ~> StringBasic).parsed
  import com.gargoylesoftware.htmlunit.WebClient
  val client = new WebClient()
  try {
    client.getPage(s"http://localhost:7357/$page")
  } finally {
    client.close()
  }
}
