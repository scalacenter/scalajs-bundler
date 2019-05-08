addSbtPlugin("org.scala-js" % "sbt-scalajs" % sys.props.getOrElse("scalajs.version", sys.error("'scalajs.version' environment variable is not defined")))

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % sys.props.getOrElse("plugin.version", sys.error("'plugin.version' environment variable is not set")))

libraryDependencies += "net.sourceforge.htmlunit" % "htmlunit" % "2.27"

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
