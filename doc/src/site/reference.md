# Reference

## `ScalaJSBundlerPlugin`

The `ScalaJSBundlerPlugin` sbt plugin automatically enables `ScalaJSPlugin` on the project. It configures
the kind of output of the project to be `ModuleKind.CommonJSModule`. Finally, it also configures its
execution environment so that npm packages are fetched (by running the `npm update` command in the
project’s target directory) before the project is `run` or `test`ed.

It is also possible to bundle the application and its dependencies into a single .js file by using
the `webpack` task scoped to a Scala.js stage (`fastOptJS` or `fullOptJS`):

~~~
> fastOptJS::webpack
~~~

The bundling process produces a single file that automatically calls the application entry
point, so there is no need for an
[additional launcher](http://www.scala-js.org/doc/project/building.html#writing-launcher-code).

### JavaScript Dependencies {#npm-dependencies}

To define the npm packages your project depends on, use the `npmDependencies` key:

~~~ scala
npmDependencies in Compile += "node-uuid" -> "~1.4.7"
~~~

You can also scope dependencies to `Test`:

~~~ scala
npmDependencies in Test += "jasmine" -> "2.5.2"
~~~

> {.note}
> Your facades must use
> [`@JSImport`](https://www.scala-js.org/doc/interoperability/facade-types.html#a-nameimporta-imports-from-other-javascript-modules)
> in order to work with the npm modules.

Last but not least, the `.js` files that are in your classpath are automatically copied to the
working directory of the `node` command. This means that you can also `@JSImport` these modules from
your Scala facades (you can see an example
[here](https://github.com/scalacenter/scalajs-bundler/blob/master/sbt-scalajs-bundler/src/sbt-test/sbt-scalajs-bundler/js-resources/src/main/scala/example/MyModule.scala#L6)).

### jsdom Support for Tests {#jsdom}

If your tests execution environment require the DOM, add the following line to your build:

~~~ scala
requiresDOM in Test := true
~~~

Then, `ScalaJSBundlerPlugin` will automatically download jsdom and bundle the tests before
their execution so that they can be loaded by jsdom.

You can find an example of project requiring the DOM for its tests
[here](https://github.com/scalacenter/scalajs-bundler/blob/master/sbt-scalajs-bundler/src/sbt-test/sbt-scalajs-bundler/static/).

### Tasks {#tasks}

`webpack`: Bundles the output of a Scala.js stage. For instance, to bundle the
output of `fastOptJS`, run the following task from the sbt shell: `fastOptJS::webpack`.
Note that to refer to this task from a build definition it must be scoped by the
current configuration, Scala.js stage and project: `webpack in (Compile, fastOptJS)`
(or `webpack in (projectRef, Compile, fastOptJS in projectRef)` to explicitly scope
it to another project that the project which is being applied settings).

`npmUpdate`: Downloads NPM dependencies. This task is also scoped to a Scala.js stage.

### Settings {#settings}

`npmDependencies`: list of the NPM packages (name and version) your application depends on.
You can use [semver](https://docs.npmjs.com/misc/semver) versions.

`npmDevDependencies`: list of the NPM packages (name and version) your build depends on.

`enableReloadWorkflow`: whether to enable the “reload workflow” for `webpack in fastOptJS`.
When enabled, dependencies are pre-bundled so that the output of `fastOptJS` can directly
be executed by a web browser without being further processed by a bundling system. This
reduces the delays when live-reloading the application on source modifications. Defaults
to `true`.

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

## `WebScalaJSBundlerPlugin`

The `WebScalaJSBundlerPlugin` provides integration with [sbt-web-scalajs](https://github.com/vmunier/sbt-web-scalajs).
Enable this plugin on JVM projects that need to use .js artifacts produced by Scala.js projects (ie in places
where you used to enable `WebScalaJS`).

The plugin tunes the `scalaJSPipeline` to use the bundles produced by webpack rather than the direct
output of the Scala.js compilation.

### Importing Assets from NPM Packages {#npm-assets}

Some NPM packages also contain static assets (e.g. fonts, stylesheets, images, etc.). You can make them available
as sbt-web assets as follows:

~~~ scala
npmAssets ++= NpmAssets.ofProject(client) { nodeModules =>
  (nodeModules / "font-awesome").***
}.value
~~~

Where `client` is the identifier of an sbt project that uses the `ScalaJSBundlerPlugin`. The above configuration
makes all the files within the `font-awesome` package available as sbt-web assets.
These assets keep their path prefix relative to the `node_modules` directory: for instance the asset path of the
`css/font-awesome.min.css` resource is `font-awesome/css/font-awesome.min.css`.

### Settings {#web-settings}

`npmAssets` Sequence of `PathMapping`s to include to sbt-web’s assets.
