import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebConsole.Logger

name := "webpack-dev-server"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

// Use a custom config file
webpackConfigFile := Some(baseDirectory.value / "webpack.config.js")

(npmDevDependencies in Compile) += ("html-webpack-plugin" -> "3.0.6")

webpackDevServerPort := 7357

version in webpack                     := "4.1.1"

version in startWebpackDevServer       := "3.1.1"

// (Used by tests only) checks that a HTML can be loaded (and that its JavaScript can be executed) without errors
InputKey[Unit]("html") := {
  import complete.DefaultParsers._
  val ags = Def.spaceDelimited().parsed.toList

  val (page, timeout, shouldPass) =
    ags match {
      case List(page, rawTimeout, pass) => (page, rawTimeout.toInt, pass.toBoolean)
      case _ => sys.error("expected: page timeout")
    }

  var totalTime = 0
  val dt = 100
  var connected = false
  var gotMessage = false

  println("Connecting to webpack-dev-server")
  println("")

  while(totalTime < timeout && !connected) {
    val expected = "6051a036-bfb4-4158-a171-950416b5bd9a"

    val client = new WebClient()
    client.getWebConsole.setLogger(new Logger(){
      def info(message: Any): Unit = {
        if (message == expected){
          gotMessage = true
        } else {
          println(s"info $message")
        }
      }
      def debug(message: Any): Unit = println(s"debug: $message")
      def error(message: Any): Unit = println(s"error: $message")
      def trace(message: Any): Unit = println(s"trace: $message")
      def warn(message: Any): Unit = println(s"warn: $message")
      def isDebugEnabled(): Boolean = true
      def isErrorEnabled(): Boolean = true
      def isInfoEnabled(): Boolean = true
      def isTraceEnabled(): Boolean = true
      def isWarnEnabled(): Boolean = true
    })

    try {
      client.getPage(s"http://localhost:7357/$page")
      connected = true
    } catch {
      case ex: org.apache.http.conn.HttpHostConnectException => {
        print("*")
      }
    } finally {
      client.close()
    }
    Thread.sleep(dt)
    totalTime += dt
  }

  assert(!shouldPass || gotMessage, "Did not get println result")
}

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
