name := "library"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.12"

npmDependencies in Compile += "uuid" -> "3.1.0"

//#relevant-settings
webpackBundlingMode := BundlingMode.LibraryAndApplication()
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

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
