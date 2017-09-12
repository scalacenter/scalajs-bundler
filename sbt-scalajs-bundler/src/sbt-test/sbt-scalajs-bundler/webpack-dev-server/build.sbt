import com.gargoylesoftware.htmlunit.WebClient

name := "webpack-dev-server"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

scalaJSUseMainModuleInitializer := true

// Use a custom config file
webpackConfigFile := Some(baseDirectory.value / "webpack.config.js")

(npmDevDependencies in Compile) += ("html-webpack-plugin" -> "2.24.1")

webpackDevServerPort := 7357

// (Used by tests only) checks that a HTML can be loaded (and that its JavaScript can be executed) without errors
InputKey[Unit]("html") := {
  import complete.DefaultParsers._
  val ags = Def.spaceDelimited().parsed.toList

  val (page, timeout) = 
    ags match {
      case List(page, rawTimeout) => (page, rawTimeout.toInt)
      case _ => sys.error("expected: page timeout")
    }

  var totalTime = 0
  val dt = 1000
  var connected = false
  var gotMessage = false
  

  while(totalTime < timeout && !connected) {
    import com.gargoylesoftware.htmlunit.WebClient
    import com.gargoylesoftware.htmlunit.WebConsole.Logger

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
      case scala.util.control.NonFatal(e) => {
        println(s"Got $e, Reconnecting")
      }
    } finally {
      client.close()
    }
    Thread.sleep(dt)
    totalTime += dt
  }

  assert(gotMessage, "Did not get println result")
}
