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

npmDevDependencies in Compile += "style-loader" -> "0.21.0"

npmDevDependencies in Compile += "css-loader" -> "0.28.11"

npmDevDependencies in Compile += "mini-css-extract-plugin" -> "0.4.0"

webpackDevServerPort := 7357

useYarn := true

version in webpack                     := "4.8.1"

version in startWebpackDevServer       := "3.1.4"

// Check that a HTML can be loaded (and that its JavaScript can be executed) without errors
InputKey[Unit]("html") := {
  import complete.DefaultParsers._
  import scala.sys.process._

  val (page, assetsCount) = (token(Space ~> StringBasic) ~ token(Space ~> IntBasic)).parsed
  import com.gargoylesoftware.htmlunit.WebClient
  val files = (webpack in (Compile, fastOptJS)).value
  assert(files.length == assetsCount)
  // Check all files are present
  assert(files.map(_.data.exists).forall(_ == true))
  // There is only one library file
  assert(files.count(_.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.Library)) == 1)
  // And 2 assets, the css and its map
  assert(files.count(_.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.Asset)) == 2)
  // The application is the first
  assert(files.head.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.Application))
  val client = new WebClient()
  try {
    val scalajsBundleDir = s"${(npmUpdate in Compile).value.absolutePath}"
    client.getPage(s"file://$scalajsBundleDir/$page")
  } finally {
    client.close()
  }
}

// Check that a HTML can be loaded on the output path specified
InputKey[Unit]("htmlProd") := {
  import complete.DefaultParsers._
  import scala.sys.process._

  val ((page, path), assetsCount) = (token(Space ~> StringBasic) ~ token(Space ~> StringBasic) ~ token(Space ~> IntBasic)).parsed
  val files = (webpack in (Compile, fullOptJS)).value
  assert(files.length == assetsCount)
  // Check all files are present
  assert(files.map(_.data.exists).forall(_ == true))
  // There is only one library file
  assert(files.count(_.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.ApplicationBundle)) == 1)
  // The rest are assets
  assert(files.count(_.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.Asset)) == assetsCount - 1)
  // The bundle is the first
  assert(files.head.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.ApplicationBundle))
  import com.gargoylesoftware.htmlunit.WebClient
  val client = new WebClient()
  try {
    val demoDir = s"${new File((baseDirectory in Compile).value, path).absolutePath}"
    client.getPage(s"file://$demoDir/$page")
  } finally {
    client.close()
  }
}

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
