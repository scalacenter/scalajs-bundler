# Getting started

## Basic setup

You need to have `npm` installed on your system.

Add the `sbt-scalajs-bundler` plugin to your Scala.js project, in your `project/plugins.sbt` file:

~~~ scala expandVars=true
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "{{version}}")
~~~

> {.note}
> Note that the plugin requires Scala.js 0.6.22+ and either
> sbt 0.13.16+ or 1.0.0-RC2+.

Enable the `ScalaJSBundlerPlugin`, in your `build.sbt` file:

~~~ scala
enablePlugins(ScalaJSBundlerPlugin)
~~~

If you have a `main` method, make sure that you enable the Scala.js main module initializer with the following setting:

~~~ scala
scalaJSUseMainModuleInitializer := true
~~~

Add dependencies to the npm packages your application requires, in your `build.sbt` file, e.g.:

~~~ scala
npmDependencies in Compile += "snabbdom" -> "0.5.3"
~~~

> {.note}
> You will most probably want to write a [Scala.js facade](https://www.scala-js.org/doc/interoperability/facade-types.html)
> for your module. You can find information on how to do that in the
> [cookbook](cookbook.md#facade), or draw inspiration from
> [this example](https://github.com/scalacenter/scalajs-bundler/blob/master/sbt-scalajs-bundler/src/sbt-test/sbt-scalajs-bundler/browserless/src/main/scala/uuid/uuid.scala).

Then, use the `fastOptJS::webpack` sbt command to download the npm packages and bundle your Scala.js
application into a JavaScript file executable by a web browser.

See complete examples in the [tests](https://github.com/scalacenter/scalajs-bundler/tree/master/sbt-scalajs-bundler/src/sbt-test/sbt-scalajs-bundler).

## Integrating with sbt-web {#sbt-web}

For sbt-web integration use the `sbt-web-scalajs-bundler` plugin instead of `sbt-scalajs-bundler`:

~~~ scala expandVars=true
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "{{version}}")
~~~

Then, enable the `WebScalaJSBundlerPlugin` on the project that uses sbt-web:

~~~ scala
lazy val server = project
  .settings(
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline)
  )
  .enablePlugins(WebScalaJSBundlerPlugin)

lazy val client = project.enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
~~~

You also need to setup the `ScalaJSBundlerPlugin` on the Scala.js project, as described in the preceding section, and
the `sbt-web-scalajs` plugins as described in [their documentation](https://github.com/vmunier/sbt-web-scalajs).

The `WebScalaJSBundlerPlugin` plugin automatically configures the `scalaJSPipeline` task to use
the bundles rather than the output of the Scala.js compilation.

You can see a complete example [here](https://github.com/scalacenter/scalajs-bundler/tree/master/sbt-web-scalajs-bundler/src/sbt-test/sbt-web-scalajs-bundler/play).
