# Reference

The sbt plugin is automatically enabled on projects where `ScalaJSPlugin` is enabled. It configures the
execution environment so that npm packages are fetched (by running the `npm update` command in the project’s
target directory) before the project is `run` or `test`ed.

It is also possible to bundle the application and its dependencies into a single .js file by using
the `webpack` task scoped to a Scala.js stage (`fastOptJS` or `fullOptJS`):

~~~
> fastOptJS::webpack
~~~

To define the npm packages your project depends on, use the `npmDependencies` key:

~~~ scala
npmDependencies in Compile += "node-uuid" -> "~1.4.7"
~~~

You can also scope dependencies to `Test`:

~~~ scala
npmDependencies in Test += "jasmine" -> "2.5.2"
~~~

> {.note}
> Your facades need to use
> [`@JSImport`](https://www.scala-js.org/doc/interoperability/facade-types.html#a-nameimporta-imports-from-other-javascript-modules)
> in order to work with the npm modules.

The two remaining sections describe the sbt tasks and settings provided by the plugin.

## Tasks {#tasks}

`webpack`: Bundles the output of a Scala.js stage. For instance, to bundle the
output of `fastOptJS`, run the following sbt task: `fastOptJS::webpack`.

`npmUpdate`: Downloads NPM dependencies. This task is also scoped to a Scala.js stage.

## Settings {#settings}

`npmDependencies`: list of the NPM packages (name and version) your application depends on.
You can use [semver](https://docs.npmjs.com/misc/semver) versions.

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