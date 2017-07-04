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
> in order to work with the npm modules, otherwise you will need some additional configuration, as explained
> [here](cookbook.md#global-namespace).

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

### Reload Workflow {#reload-workflow}

Each time you change something in your application source code and compile the project, Scala.js emits a new .js file 
that can weigh several MBs (in `fastOptJS` mode) if your application is large. Turning this huge CommonJS module into 
code executable by web browsers takes time. This can be a problem if you rely on a “live-reloading” workflow 
(like the one of Play framework, for instance), because the reloading time can go up to 30 seconds.

You can get a faster “change source and reload application” workflow by setting the `enableReloadWorkflow` 
key to `true`. An alternative way to invoke reload workflow is using the `fastOptJs::webpackReload` task.

The reload workflow replaces the `fastOptJS::webpack` task implementation with a different one, that does not use 
webpack to process the output of the Scala.js compilation.  Instead, it pre-bundles the modules imported by your 
application and exposes them to the global namespace. Since these dependencies can then be resolved from the global 
namespace, the output of Scala.js is just concatenated after the contents of the pre-bundling process.

It is possible to configure an alternative webpack configuration file which is used for building the bundle using the
`webpackConfigFile in webpackReload` setting . The configuration file may not contain `entry` nor `output` configuration
but can be used to for loaders etc.

### Tasks and Settings {#tasks-and-settings}

The tasks and settings that control the plugin are documented in the API documentation
of the [ScalaJSBundlerPlugin](api:scalajsbundler.sbtplugin.ScalaJSBundlerPlugin$).

## `WebScalaJSBundlerPlugin`

The `WebScalaJSBundlerPlugin` provides integration with [sbt-web-scalajs](https://github.com/vmunier/sbt-web-scalajs).
Enable this plugin on JVM projects that need to use .js artifacts produced by Scala.js projects (ie in places
where you used to enable `WebScalaJS`).

The plugin tunes the `scalaJSPipeline` to use the bundles produced by webpack rather than the direct
output of the Scala.js compilation.

> {.note}
> The [scalajs-scripts](https://github.com/vmunier/scalajs-scripts) will **not** work anymore because they
> try to include the output of Scala.js instead of the output of the bundling process. You can see
> [here](https://github.com/scalacenter/scalajs-bundler/blob/master/sbt-web-scalajs-bundler/src/sbt-test/sbt-web-scalajs-bundler/play/server/src/main/scala/example/ExampleController.scala#L25-L30)
> how to include the correct .js file.

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
