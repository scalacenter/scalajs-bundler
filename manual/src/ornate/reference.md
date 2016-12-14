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

### Yarn {#yarn}

By default, `npm` is used to fetch the dependencies but you can use [Yarn](https://yarnpkg.com/) by setting the
`useYarn` key to `true`:

~~~ scala
useYarn := true
~~~

The `yarn` command must be available in the host platform.

### Tasks and Settings {#tasks-and-settings}

The tasks and settings that control the plugin are documented in the API documentation
of the [ScalaJSBundlerPlugin](api:scalajsbundler.sbtplugin.ScalaJSBundlerPlugin$).

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

### Tasks and Settings {#web-tasks-and-settings}

The tasks and settings that control the plugin are documented in the API documentation
of the [WebScalaJSBundlerPlugin](api:scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin$).
