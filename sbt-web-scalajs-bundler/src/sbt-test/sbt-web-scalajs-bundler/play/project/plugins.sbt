val scalaJSVersion = sys.props.getOrElse("scalajs.version", sys.error("'scalajs.version' environment variable is not defined"))
val scalaJSBundlerVersion = sys.props.getOrElse("plugin.version", sys.error("'plugin.version' environment variable is not set"))

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % scalaJSBundlerVersion)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
