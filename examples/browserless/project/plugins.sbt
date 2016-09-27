addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.13-SNAPSHOT")

lazy val `scalajs-bundler` = RootProject(file("../../../sbt-plugin"))

val build = project.in(file(".")).dependsOn(`scalajs-bundler`)
