addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.13-SNAPSHOT")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % System.getProperty("plugin.version"))

libraryDependencies += "net.sourceforge.htmlunit" % "htmlunit" % "2.23"