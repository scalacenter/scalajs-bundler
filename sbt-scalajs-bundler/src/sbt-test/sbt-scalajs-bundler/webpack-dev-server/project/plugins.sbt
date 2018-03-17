addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % sys.props.getOrElse("plugin.version", sys.error("'plugin.version' environment variable is not set")))

libraryDependencies += "net.sourceforge.htmlunit" % "htmlunit" % "2.27"
