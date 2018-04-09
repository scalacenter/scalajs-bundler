import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebConsole.Logger

name := "webpack-assets"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.12.5"

scalaJSUseMainModuleInitializer := true

//webpackBundlingMode := BundlingMode.LibraryAndApplication()

// Use a custom config file
webpackConfigFile := Some(baseDirectory.value / "webpack.config.js")

npmDevDependencies in Compile += "html-webpack-plugin" -> "3.0.6"

npmDevDependencies in Compile += "webpack-merge" -> "4.1.2"

webpackDevServerPort := 7357

version in webpack                     := "4.3.0"

version in startWebpackDevServer       := "3.1.1"

// Check that a HTML can be loaded (and that its JavaScript can be executed) without errors
InputKey[Unit]("html") := {
  import complete.DefaultParsers._
  import scala.sys.process._

  val page = (Space ~> StringBasic).parsed
  import com.gargoylesoftware.htmlunit.WebClient
  val client = new WebClient()
  try {
    val scalajsBundleDir = s"${(npmUpdate in Compile).value.absolutePath}"
    client.getPage(s"file://$scalajsBundleDir/$page")
  } finally {
    client.close()
  }
}
