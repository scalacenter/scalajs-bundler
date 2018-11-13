import com.gargoylesoftware.htmlunit.WebClient

name := "global-namespace"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

libraryDependencies += "com.github.japgolly.scalajs-react" %%% "core" % "0.11.3"

//#relevant-settings
// Resolve the required JS dependencies from NPM
npmDependencies in Compile ++= Seq(
  "react" -> "15.4.1",
  "react-dom" -> "15.4.1"
)

// Add a dependency to the expose-loader (which will expose react to the global namespace)
npmDevDependencies in Compile += "expose-loader" -> "0.7.5"

// Use a custom config file to export the JS dependencies to the global namespace,
// as expected by the scalajs-react facade
webpackConfigFile := Some(baseDirectory.value / "webpack.config.js")

//#relevant-settings

// (Used by tests only) checks that a HTML can be loaded (and that its JavaScript can be executed) without errors
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
