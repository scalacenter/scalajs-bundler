scalajs-bundler
===============

Module bundler for Scala.js projects that use npm packages. Uses
[webpack](https://webpack.github.io/) under the hood.

## Usage

Add the `scalajs-bundler` plugin to your Scala.js project, in your `project/plugins.sbt` file:

~~~ scala
addSbtPlugin("ch.epfl.scala" % "scalajs-bundler" % "0.1")
~~~

Set the `scalaJSModuleKind` setting to `NodeJSModule`, in your `build.sbt` file:

~~~ scala
scalaJSModuleKind := ModuleKind.NodeJSModule
~~~

Add dependencies to the npm packages your application requires, in your `build.sbt` file:

~~~ scala
npmDependencies += "snabbdom" -> "0.5.3"
~~~

Then, use the `bundle` sbt command to download the npm packages and bundle your Scala.js
application into a JavaScript file executable by a Web browser.

See complete examples in the [`examples`](examples) directory.

## Reference

The plugin introduces the following tasks and settings.

### Tasks

`bundle`: Bundles the output of `fastOptJS`.

`bundleOpt`: Bundles the output of `fullOptJS`.

### Settings

`npmDependencies`: list of the npm packages (name and version) your application depends on.

`npmDevDependencies`: list of the npm packages (name and version) your build depends on.

`webpackVersion`: version of webpack to use.

`webpackConfigFile`: configuration file to use with webpack. By default, the plugin generates a
configuration file, but you can supply your own file via this setting.

`webpackSourceMap`: whether to enable (or not) source-map.

## TODO

- source-map support
- caching
- logs
- code splitting support

## License

This content is released under the [MIT License](http://opensource.org/licenses/mit-license.php).
