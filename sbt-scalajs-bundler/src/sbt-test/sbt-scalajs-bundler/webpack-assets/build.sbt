import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebConsole.Logger

name := "webpack-assets"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.13.2"

scalaJSUseMainModuleInitializer := true

// Use library mode for fastOptJS
webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly()

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "dev.config.js")

// Use application model mode for fullOptJS
webpackBundlingMode in fullOptJS := BundlingMode.Application

webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.config.js")

npmDevDependencies in Compile += "html-webpack-plugin" -> "5.2.0"

npmDevDependencies in Compile += "webpack-merge" -> "5.7.3"

npmDevDependencies in Compile += "style-loader" -> "2.0.0"

npmDevDependencies in Compile += "css-loader" -> "5.0.1"

npmDevDependencies in Compile += "mini-css-extract-plugin" -> "1.3.4"

webpackDevServerPort := 7357

jsPackageManager := scalajsbundler.PackageManager.Yarn()

// HtmlUnit does not support ECMAScript 2015
scalaJSLinkerConfig ~= { _.withESFeatures(_.withUseECMAScript2015(false)) }

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
  // There is only one app file
  assert(files.count(_.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.Application)) == 1)
  // There is only one library file
  assert(files.count(_.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.Library)) == 1)
  // There is only one library file
  assert(files.count(_.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.Loader)) == 1)
  // And 1 asset (HTML file)
  assert(files.count(_.metadata.get(BundlerFileTypeAttr) == Some(BundlerFileType.Asset)) == 1)
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
