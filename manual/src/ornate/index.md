# scalajs-bundler

scalajs-bundler is a module bundler for Scala.js projects that use npm packages: it bundles the .js file
emitted by the Scala.js compiler with its npm dependencies into a single .js file executable by Web browsers.

scalajs-bundler uses [npm](https://www.npmjs.com) and [webpack](https://webpack.github.io/) under the hood.

Last stable version is ![](config:version):

~~~ scala expandVars=true
// For Scala.js 1.x
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "{{version}}")
// Or, for Scala.js 0.6.x
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler-sjs06" % "{{version}}")
~~~

If you're using [sbt-crossproject](https://github.com/portable-scala/sbt-crossproject) you need to add plugin via `jsConfigure`:

~~~ scala expandVars=true
lazy val cross = crossProject(JSPlatform, JVMPlatform).in(file("."))
  .jvmSettings(BuildSettings.jvmSettings)
  .jsSettings(BuildSettings.jsSettings)
  .jsConfigure { project => project.enablePlugins(ScalaJSBundlerPlugin) }
~~~

See the [**getting started**](getting-started.md) page for more details about
the setup process.
