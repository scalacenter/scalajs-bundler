addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.13-SNAPSHOT")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % Option(System.getProperty("plugin.version")).getOrElse(sys.error("'plugin.version' environment variable is not set")))
