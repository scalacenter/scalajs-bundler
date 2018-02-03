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
npmDependencies in Compile += "uuid" -> "~3.1.0"
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
jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
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

Yarn 0.22.0+ must be available on the host platform.

### Bundling Mode {#bundling-mode}

Each time you change something in your application source code and compile the project, Scala.js emits a new .js 
file that can weigh several MBs if your application is large. Scalajs-bundler provides
a few different options with respect to handling this large output file, controlled by setting the optional
`webpackBundlingMode` key and can be scoped to a Scala.js stage(`fastOptJS` or `fullOptJS`).

#### Application (default) {#bundling-mode-application}

`webpackBundlingMode := BundlingMode.Application` generates a webpack config that simply processes the Scala.js output file as an 
entrypoint. This means that webpack loaders, plugins, etc will work as usual. It also means that webpack will 
have to process a very large Scala.Js output file.

Turning this CommonJS module into code executable by web browsers takes time, often upwards of a minute. 

Nonetheless, this is identical to what you'd get if were to duplicate your workflow outside scalajs-bundler, 
so it remains the default. 

#### Library Only {#bundling-mode-library-only}
You can get a much faster “change source and reload application” workflow by setting the 
`webpackBundlingMode := BundlingMode.LibraryOnly()` key. This bundling mode avoids having webpack process the entire
Scala.js output, but instead uses webpack to bundle all the javascript dependencies (determined via `@JSImport` 
and any changes to the `webpack.config.js`). This is accomplished by setting the webpack `output.library` and 
`output.libraryTarget` keys in the webpack.config. By default, this generates a global variable named
`ScalaJSBundlerLibrary`, but if needed, that variable name can be overridden via the `exportedName` 
parameter provided to `BundlingMode.LibraryOnly`.

It then uses a special "loader" to resolve these dependencies for the actual Scala.js application.

In order to use this mode, instead of including `yourapp-bundle.js` in your page, you will need to include
`yourapp-library.js`, `yourapp-loader.js`, `yourapp-fastopt.js` or `yourapp-opt.js`. If you're using Play 
Framework, you could use a twirl template similar to this one:

~~~ html
@(projectName: String,
  assets: String => String,
  resourceExists: String => Boolean,
  htmlAttributes: Html = Html("")
)

@defining(s"${projectName.toLowerCase}") { name =>
  @Seq(s"$name-opt-library.js", s"$name-fastopt-library.js").find(resourceExists).map(name => jsScript(assets(name), htmlAttributes))
}
<script language="JavaScript">
var exports = window;
exports.require = window["ScalaJSBundlerLibrary"].require;
</script>
@defining(s"${projectName.toLowerCase}") { name =>
  @Seq(s"$name-opt.js", s"$name-fastopt.js").find(resourceExists).map(name => jsScript(assets(name), htmlAttributes))
}
~~~

The default variable global for the library is `ScalaJsBundlerDependencies`. Should you need to change it, 
you can provide a new variable name to the configuration, such as `webpackBundlingMode := BundlingMode.LibrariesOnly("myLib")`.

##### Benefits

By avoiding processing the entire Scala.js output file, webpack times from 10's of seconds to minutes to 
a couple of seconds, depending on how many dependencies webpack has to resolve. In addition, since the 
module references are all still managed by webpack, any custom loaders such as the `html-loader` or `text-loader`
will work as usual. Other webpack configuration such as external modules also work out of the box.

By splitting the output into a separate library and Scala.js app we are able to leverage browser caching for most
reloads, since it's seldom that both .js assets and Scala.js assets change at the same time. 

By avoiding any post-processing of the Scala.js output, we leave the sourcemap generated by the Scala.js compiler
entirely intact. This means we avoid any translation or lookup errors introduced by webpack, and also means
we save a bunch more processing time even if sourcemaps are enabled.

##### How It Works
Scalajs-bundler has a unique advantage in the JavaScript build ecosystem in that it runs inside SBT and has
access to the Scala.Js compiler internals. By leveraging these, we can generate a fake `entrypoint.js` file that 
represents all the `@JSImport`s from a Scala.JS project. We can use this entrypoint to generate a library file
that provides access to all those imports without webpack having to ever process the entire Scala.js output. 

Inside the `entrypoint.js` file we also provide a trivial `require` implementation that provides access to the
modules which were imported.

All that remains is to provide an `exports` variable with a reference to the `require` implementation
for the Scala.js out to hang onto, which is what the loader provides.

#### Library and Application {#bundling-mode-library-and-application}

`bundlingMode := BundlingMode.LibraryAndApplication()` builds on `BundlingMode.LibraryOnly` and attempts to 
duplicate the output of `BundlingMode.Application` without the overhead processing the entire Scala.js output file. 
It uses the same library file generation process as `BundlingMode.LibraryOnly`. It then bundles that library, 
the loader, and the Scala.js output into a `yourapp-bundle` file by concatenating them. If `enableSourceMaps := true`, 
it will attempt to use the node.js `concat-with-sourcemaps` module to combine the sourcemaps as well. 

If you need interoperability with the full `Application` mode or somehow can't handle the two files generated
by the `LibraryOnly()` mode, this mode may be useful for you. However, it relies on post-processing and eliminates
some of the benefits of the `LibraryOnly` mode, so consider carefully if you really need it before turning it on.

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
