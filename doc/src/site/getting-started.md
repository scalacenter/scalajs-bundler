# Getting started

## Basic setup

You need to have [npm](https://docs.npmjs.com/getting-started/installing-node) installed on your
system.

Add the `sbt-scalajs-bundler` plugin to your Scala.js project, in your `project/plugins.sbt` file:

~~~ scala expandVars=true
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "{{version}}")
~~~

> Note that the plugin requires Scala.js 0.6.13+.

Set the `scalaJSModuleKind` setting to `CommonJSModule`, in your `build.sbt` file:

~~~ scala
scalaJSModuleKind := ModuleKind.CommonJSModule
~~~

Add dependencies to the npm packages your application requires, in your `build.sbt` file, e.g.:

~~~ scala
npmDependencies in Compile += "snabbdom" -> "0.5.3"
~~~

Then, use the `fastOptJS::webpack` sbt command to download the npm packages and bundle your Scala.js
application into a JavaScript file executable by a Web browser.

See complete examples in the [`tests`](https://github.com/scalacenter/scalajs-bundler/tree/master/sbt-scalajs-bundler/src/sbt-test/sbt-scalajs-bundler).

## Integrating with sbt-web {#sbt-web}

For sbt-web integration use the `sbt-web-scalajs-bundler` plugin instead of `sbt-scalajs-bundler`:

~~~ scala expandVars=true
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "{{version}}")
~~~

You also need to setup the [`sbt-web-scalajs`](https://github.com/vmunier/sbt-web-scalajs) plugins
as described in their documentation.

The `sbt-web-scalajs-bundler` plugin automatically configures the `scalaJSPipeline` task to use
the bundles rather than the output of the Scala.js compilation.

The other steps are the same as in the basic setup.

You can see a complete example [here](https://github.com/scalacenter/scalajs-bundler/tree/master/sbt-web-scalajs-bundler/src/sbt-test/sbt-web-scalajs-bundler/play).
