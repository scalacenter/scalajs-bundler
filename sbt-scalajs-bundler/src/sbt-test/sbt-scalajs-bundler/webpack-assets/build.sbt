import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebConsole.Logger

name := "webpack-assets"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.12.5"

scalaJSUseMainModuleInitializer := true

// Use library mode for fastOptJS
webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly()

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "dev.config.js")

// Use application model mode for fullOptJS
webpackBundlingMode in fullOptJS := BundlingMode.Application

webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.config.js")

npmDevDependencies in Compile += "html-webpack-plugin" -> "3.0.6"

npmDevDependencies in Compile += "webpack-merge" -> "4.1.2"

webpackDevServerPort := 7357

useYarn := true

version in webpack                     := "4.6.0"

version in startWebpackDevServer       := "3.1.3"

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
