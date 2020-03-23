val scalaJSVersion = sys.props.getOrElse("scalajs.version", sys.error("'scalajs.version' environment variable is not defined"))
val scalaJSBundlerVersion = sys.props.getOrElse("plugin.version", sys.error("'plugin.version' environment variable is not set"))
val pluginSuffix = if (scalaJSVersion.startsWith("1.")) "" else "-sjs06"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

addSbtPlugin("ch.epfl.scala" % s"sbt-scalajs-bundler$pluginSuffix" %  scalaJSBundlerVersion)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
