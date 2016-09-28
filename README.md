sbt-scalajs-bundler
===============

Module bundler for Scala.js projects that use NPM packages.

Uses [npm](https://www.npmjs.com) and [webpack](https://webpack.github.io/) under the hood.

## Getting Started

You need to have [npm](https://docs.npmjs.com/getting-started/installing-node) installed on your system.

Add the `scalajs-bundler` plugin to your Scala.js project, in your `project/plugins.sbt` file:

~~~ scala
addSbtPlugin("ch.epfl.scala" % "scalajs-bundler" % <version>)
~~~

Set the `scalaJSModuleKind` setting to `NodeJSModule`, in your `build.sbt` file:

~~~ scala
scalaJSModuleKind := ModuleKind.NodeJSModule
~~~

Add dependencies to the npm packages your application requires, in your `build.sbt` file:

~~~ scala
npmDependencies in Compile += "snabbdom" -> "0.5.3"
~~~

Then, use the `fastOptJS::webpack` sbt command to download the npm packages and bundle your Scala.js
application into a JavaScript file executable by a Web browser.

See complete examples in the [`tests`](src/sbt-test/sbt-scalajs-bundler/).

## Reference

The plugin introduces the following tasks and settings.

### Tasks

`webpack`: Bundles the output of a Scala.js stage. Example: `> fastOptJS::webpack`.

`npmUpdate`: Downloads NPM dependencies.

### Settings

`npmDependencies`: list of the NPM packages (name and version) your application depends on.

`npmDevDependencies`: list of the NPM packages (name and version) your build depends on.

`version in webpack`: version of webpack to use.

`webpackConfigFile`: configuration file to use with webpack. By default, the plugin generates a
configuration file, but you can supply your own file via this setting.

`emitSourceMaps in webpack`: whether to enable (or not) source-map.

## TODO

- source-map support
- caching
- code splitting support

## License

This content is released under the [MIT License](http://opensource.org/licenses/mit-license.php).
