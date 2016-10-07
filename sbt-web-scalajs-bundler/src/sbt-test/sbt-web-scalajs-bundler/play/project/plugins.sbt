addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.8")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.13-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.1")

addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % Option(System.getProperty("plugin.version")).getOrElse(sys.error("'plugin.version' environment variable is not set")))
