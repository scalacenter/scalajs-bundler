# Reference

The plugin introduces the following tasks and settings.

## Tasks {#tasks}

`webpack`: Bundles the output of a Scala.js stage. For instance, to bundle the
output of `fastOptJS`, run the following sbt task: `fastOptJS::webpack`.

`npmUpdate`: Downloads NPM dependencies. This task is also scoped to a Scala.js stage.

## Settings {#settings}

`npmDependencies`: list of the NPM packages (name and version) your application depends on.
You can use [semver](https://docs.npmjs.com/misc/semver) versions. Example of use:

~~~ scala
npmDependencies in Compile += "node-uuid" -> "~1.4.7"
~~~

`npmDevDependencies`: list of the NPM packages (name and version) your build depends on.

`webpackEntries`: list of entry bundles to generate. By default it generates just one bundle
for your main class.

`version in webpack`: version of webpack to use. Example of use:

~~~ scala
version in webpack := "2.1.0-beta.25"
~~~

`webpackConfigFile`: configuration file to use with webpack. By default, the plugin generates a
configuration file, but you can supply your own file via this setting. Example of use:

~~~ scala
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "my.prod.webpack.config.js")
~~~

You can find more insights on how to write a custom configuration file in the [cookbook](cookbook.md#custom-config).

`webpackEmitSourceMaps in (<configuration>, <stage>)`: whether to enable (or not) source-map in
the given configuration (`Compile` or `Test`) and stage (`fastOptJS` or `fullOptJS`). Example
of use:

~~~ scala
webpackEmitSourceMaps in (Compile, fullOptJS) := false
~~~

Note that, by default, this setting has the same value as `emitSourceMaps`, so, to globally
disable source maps you can just configure the `emitSourceMaps` setting:

~~~ scala
emitSourceMaps := false
~~~