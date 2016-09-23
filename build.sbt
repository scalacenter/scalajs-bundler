val `sbt-plugin` = project.in(file("sbt-plugin"))

val `scalajs-bundler` = project.in(file(".")).aggregate(`sbt-plugin`)