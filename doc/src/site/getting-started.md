# Getting started

## Basic setup

You need to have [npm](https://docs.npmjs.com/getting-started/installing-node) installed on your
system.

Add the `sbt-scalajs-bundler` plugin to your Scala.js project, in your `project/plugins.sbt` file:

~~~ scala expandVars=true
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "{{version}}")
~~~

> {.note}
> Note that the plugin requires Scala.js 0.6.13+.

Enable the `ScalaJSBundlerPlugin`, in your `build.sbt` file:

~~~ scala
enablePlugins(ScalaJSBundlerPlugin)
~~~

Add dependencies to the npm packages your application requires, in your `build.sbt` file, e.g.:

~~~ scala
npmDependencies in Compile += "snabbdom" -> "0.5.3"
~~~

> {.note}
> You will most probably want to write a Scala.js facade for your module. You can get some help in the Scala.js
> [documentation](https://www.scala-js.org/doc/interoperability/facade-types.html#a-nameimporta-imports-from-other-javascript-modules),
> or have a look at
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
enablePlugins(WebScalaJSBundlerPlugin)
~~~

You also need to setup the `ScalaJSBundlerPlugin` on the Scala.js project, as described in the preceding section, and
the `sbt-web-scalajs` plugins as described in [their documentation](https://github.com/vmunier/sbt-web-scalajs).

The `WebScalaJSBundlerPlugin` plugin automatically configures the `scalaJSPipeline` task to use
the bundles rather than the output of the Scala.js compilation.

You can see a complete example [here](https://github.com/scalacenter/scalajs-bundler/tree/master/sbt-web-scalajs-bundler/src/sbt-test/sbt-web-scalajs-bundler/play).
