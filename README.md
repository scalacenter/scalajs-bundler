sbt-scalajs-bundler [![](https://index.scala-lang.org/scalacenter/sbt-scalajs-bundler/sbt-scalajs-bundler/latest.svg)](https://index.scala-lang.org/scalacenter/sbt-scalajs-bundler)
==================

Module bundler for Scala.js projects that use NPM packages.

Uses [npm](https://www.npmjs.com) and [webpack](https://webpack.github.io/) under the hood.

## Getting Started

You need to have [npm](https://docs.npmjs.com/getting-started/installing-node) installed on your system.

Add the `sbt-scalajs-bundler` plugin to your Scala.js project, in your `project/plugins.sbt` file:

~~~ scala
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % <version>)
~~~

> Note that the plugin requires Scala.js 0.6.13+.

Set the `scalaJSModuleKind` setting to `CommonJSModule`, in your `build.sbt` file:

~~~ scala
scalaJSModuleKind := ModuleKind.CommonJSModule
~~~

Add dependencies to the npm packages your application requires, in your `build.sbt` file:

~~~ scala
npmDependencies in Compile += "snabbdom" -> "0.5.3"
~~~

Then, use the `fastOptJS::webpack` sbt command to download the npm packages and bundle your Scala.js
application into a JavaScript file executable by a Web browser.

See complete examples in the [`tests`](sbt-scalajs-bundler/src/sbt-test/sbt-scalajs-bundler/).

## Integration with sbt-web

For sbt-web integration use the following sbt plugin instead of `sbt-scalajs-bundler`:

~~~ scala
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % <version>)
~~~

This plugin is automatically triggered if the [`sbt-web-scalajs`](https://github.com/vmunier/sbt-web-scalajs) plugin
is enabled. It then configures the `scalaJSPipeline` task to use the bundles.

You can see a complete example [here](sbt-web-scalajs-bundler/src/sbt-test/sbt-web-scalajs-bundler/play).

## Reference

The plugin introduces the following tasks and settings.

### Tasks

`webpack`: Bundles the output of a Scala.js stage. Example: `> fastOptJS::webpack`.

`npmUpdate`: Downloads NPM dependencies.

### Settings

`npmDependencies`: list of the NPM packages (name and version) your application depends on.

`npmDevDependencies`: list of the NPM packages (name and version) your build depends on.

`webpackEntries`: list of entry bundles to generate. By default it generates just one bundle for your main class.

`version in webpack`: version of webpack to use.

`webpackConfigFile`: configuration file to use with webpack. By default, the plugin generates a
configuration file, but you can supply your own file via this setting.

`emitSourceMaps in (webpack in <stage>)`: whether to enable (or not) source-map in the given stage (`fastOptJS` or `fullOptJS`).

## License

This content is released under the [MIT License](http://opensource.org/licenses/mit-license.php).
