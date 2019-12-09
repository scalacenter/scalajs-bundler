# Changelog

## Version 0.16.0

> 2019 Dec 09

The main highlight of this release is the support of Scala.js 1.0.0-RC1 instead of 1.0.0-M7.
We still support Scala.js 0.6.x, but we require at least version 0.6.31.

- Add support for Scala.js 1.0.0-RC1 (drop support for 1.0.0-M7)
- Require Scala.js 0.6.31 or later in the 0.6.x branch
- Require sbt 1.2.1 or later in the sbt 1.x branch (sbt 0.13.17+ is still supported)

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.15.0...v0.16.0).

## Version 0.15.0

> 2019 May 21

The main highlight of this release is the support of Scala.js 1.x. We still support Scala.js 0.6.x, but
we require at least version 0.6.26.

New features:
- [#201](https://github.com/scalacenter/scalajs-bundler/issues/201): Introduce a `jsSourceDirectories` setting,
  which points to a list of directories containing files (`.js`, `.json`, etc.) that can be used by Scala.js
  projects.
- [#246](https://github.com/scalacenter/scalajs-bundler/issues/246): Support Scala.js 1.0.0-M7.

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.14.0...v0.15.0).

## Version 0.14.0

> 2018 Nov 5

This release modifies the `npmUpdate` task and splits the logic into two separate tasks; `npmInstallDependencies` and
`npmInstallJSResources`. `npmUpdate` has a less obvious side effect that, not only does it run `npm install`, it would
also copy all the JavaScript resources to the `node_modules` directory. This behaviour is fine except that it is not
suitable for use in `sourceGenerators` and would cause a cycle in the tasks. `npmInstallDependencies` should be used in
cases where you want to want to use a npm module from a sbt task.

This fixes the following bugs:
 - [#258](https://github.com/scalacenter/scalajs-bundler/issues/258): Unable to use npmUpdate in sourceGenerators
 - [#261](https://github.com/scalacenter/scalajs-bundler/issues/261): Support jsdom v12.x
 - [#267](https://github.com/scalacenter/scalajs-bundler/issues/267): Support JDK9+

New features:
  - [#264](https://github.com/scalacenter/scalajs-bundler/issues/264): Ability to set `node` [flags](https://nodejs.org/api/cli.html)
  - [#266](https://github.com/scalacenter/scalajs-bundler/issues/266): Custom setting for DOM enabled `JSEnv` in `test`. (`requiresDOM` is deprecated)

And documentation fixes:
  - [#269](https://github.com/scalacenter/scalajs-bundler/issues/269): Update docs

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.13.1...v0.14.0).

## Version 0.13.1

> 2018 Jul 13

This fixes the following bugs:

  - [#224](https://github.com/scalacenter/scalajs-bundler/issues/224): Use project-level custom NPM registry

The following PRs are included in this release

- [#254](https://github.com/scalacenter/scalajs-bundler/pull/254): Npm/yarn args
- [#251](https://github.com/scalacenter/scalajs-bundler/pull/251): Fix typo
- [#249](https://github.com/scalacenter/scalajs-bundler/pull/249): Sync yarn.lock between baseDir and installDir

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.13.0...v0.14.1).

## Version 0.13.0

> 2018 Jun 5

This release contains an important revamp on the way webpack support works.
Webpack produces a json [stats output](https://webpack.js.org/api/stats) which
describes in detail the results of processing your application. Thus, parsing
stats we can learn exactly what files are produced rather than guessing.

Due to the very nature of parsing the output, and the wide variety of webpack
configurations this process may not work in all cases. Please lets us know if
you find any issues.

**Note:** `Stats` parsing has been tested mostly in Webpack 4. The results may vary with
older versions.

**Note:** If your webpack produces any kind of std output, parsing stats will likely break.

This fixes the following bugs:

- [#192](https://github.com/scalacenter/scalajs-bundler/issues/192): Webpack failed to create application bundle
- [#111](https://github.com/scalacenter/scalajs-bundler/issues/111): Lots of warnings about source map URLs

The following PRs are included in this release

- [#247](https://github.com/scalacenter/scalajs-bundler/pull/247): Assets to sbt
- [#242](https://github.com/scalacenter/scalajs-bundler/pull/242): Better error display when the webpack call fails
- [#241](https://github.com/scalacenter/scalajs-bundler/pull/241): Use Public path from webpack stats
- [#240](https://github.com/scalacenter/scalajs-bundler/pull/240): Update concat-with-sourcemaps
- [#239](https://github.com/scalacenter/scalajs-bundler/pull/239): Bugfix parsing errors on the output
- [#238](https://github.com/scalacenter/scalajs-bundler/pull/238): Fix thread leak
- [#237](https://github.com/scalacenter/scalajs-bundler/pull/237): add function as a module example
- [#234](https://github.com/scalacenter/scalajs-bundler/pull/234): Read application asset name from stats

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.12.0...v0.13.0).

## Version 0.12.0

> 2018 March 27

- [#223](https://github.com/scalacenter/scalajs-bundler/pull/223): Webpack4 support

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.11.0...v0.12.0).

## Version 0.11.0

> 2018 March 15

- [#228](https://github.com/scalacenter/scalajs-bundler/pull/228): Implements npmUdpdate in a separate object;
- [#225](https://github.com/scalacenter/scalajs-bundler/pull/225): Add --mutex to yarn command;
- [#222](https://github.com/scalacenter/scalajs-bundler/pull/222): Make library example work in OSX;
- [#220](https://github.com/scalacenter/scalajs-bundler/pull/220): Support passing an extra list of arguments to webpack;
- [#218](https://github.com/scalacenter/scalajs-bundler/pull/218): Fix [#136](https://github.com/scalacenter/scalajs-bundler/issues/136): Add more precise jsdom detection:
- [#216](https://github.com/scalacenter/scalajs-bundler/pull/216): Fix [#200](https://github.com/scalacenter/scalajs-bundler/issues/200): current webpack devserver version does not accept watchOptions;
- [#215](https://github.com/scalacenter/scalajs-bundler/pull/215): Fix [#99](https://github.com/scalacenter/scalajs-bundler/issues/99): Relax NPM dependency version conflict resolution;
- [#213](https://github.com/scalacenter/scalajs-bundler/pull/213): Fix [#168](https://github.com/scalacenter/scalajs-bundler/issues/168): Update the snabbdom facade for Scala.js 1.0

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.10.0...v0.11.0).

## Version 0.10.0

> 2018 January 31

This release requires sbt 0.13.16+ and adds support for Scala.js 0.6.22.

- [#210](https://github.com/scalacenter/scalajs-bundler/pull/210): Bundler doesn't support version of jsdom more than 9;
- [#185](https://github.com/scalacenter/scalajs-bundler/pull/185): Correct webpackBundlingMode key in docs;
- [#212](https://github.com/scalacenter/scalajs-bundler/pull/212): Migrate to sbt-scalajs 0.6.22;

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.9.0...v0.10.0).

## Version 0.9.0

> 2017 October 12

When upgrading to this release, make sure to migrate your webpack configuration to [webpack 3.X](https://webpack.js.org/guides/migrating/).

This version works with both sbt version 0.13 and 1.0. In order to support sbt 1.0,
Scala.js has been upgraded to [0.6.19](https://www.scala-js.org/news/2017/07/29/announcing-scalajs-0.6.19/).

- [#175](https://github.com/scalacenter/scalajs-bundler/pull/175): Set webpack 3.X as default version;
- [#179](https://github.com/scalacenter/scalajs-bundler/pull/179): Cross publish for sbt 1.0;
- [#176](https://github.com/scalacenter/scalajs-bundler/pull/176): Run webpack-dev-server from the scalajs-bundler folder;
- [#177](https://github.com/scalacenter/scalajs-bundler/pull/176): Scope webpackBundlingMode per Scala.js stage (`fastOptJS` or `fullOptJS`);

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.8.0...v0.9.0).

## Version 0.8.0

> 2017 September 10

When upgrading to this release, make sure to check out the new [webpackBundlingMode](reference.md#bundling-mode)
configuration value. The old `enableReloadWorkflow` key has been removed, in favor
of `BundlingMode.LibraryAndApplication()`.

- [#143](https://github.com/scalacenter/scalajs-bundler/pull/143): Document the compatible versions of npm;
- [#146](https://github.com/scalacenter/scalajs-bundler/pull/146): Document how to use global modules with jsdom in tests;
- [#149](https://github.com/scalacenter/scalajs-bundler/pull/149): Introduce `webpackBundlingMode` to finely control whether to bundle the output of Scala.js or not;
- [#153](https://github.com/scalacenter/scalajs-bundler/pull/153): Use the non interactive mode of Yarn;
- [#161](https://github.com/scalacenter/scalajs-bundler/pull/161): Set `"private": true` in generated `package.json` file to eliminate errors from npm;
- [#162](https://github.com/scalacenter/scalajs-bundler/pull/162): Differentiate between missing and unsupported Webpack versions;
- [#166](https://github.com/scalacenter/scalajs-bundler/pull/166): Move to Travis-CI;
- [#167](https://github.com/scalacenter/scalajs-bundler/pull/167): Upgrade tests that use `uuid`;
- [#171](https://github.com/scalacenter/scalajs-bundler/pull/171): Add `scalaJSProjects` resource directories to `monitoredScalaJSDirectories`;
- [#172](https://github.com/scalacenter/scalajs-bundler/pull/172): Use `npm install` command instead of `npm update`.

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.7.0...v0.8.0).

## Version 0.7.0

> 2017 July 4

When upgrading to this release, make sure to enable `scalaJSUseMainModuleInitializer := true` in your build if you have a `main` method.

- [#125](https://github.com/scalacenter/scalajs-bundler/pull/125): Copy `webpackResources` only if a custom webpack config file is used ;
- [#126](https://github.com/scalacenter/scalajs-bundler/pull/126): Ability to add custom `package.json` entries ;
- [#129](https://github.com/scalacenter/scalajs-bundler/pull/129): Generate a JavaScript array of webpack entries rather than a string ;
- [#140](https://github.com/scalacenter/scalajs-bundler/pull/140): Upgrade to Scala.js 0.6.18 ;
- [#141](https://github.com/scalacenter/scalajs-bundler/pull/141): Handle `ImportWithGlobalFallback` in reload workflow.

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.6.0...v0.7.0).

## Version 0.6.0

> 2017 April 26

- [#96](https://github.com/scalacenter/scalajs-bundler/pull/96): webpack-dev-server integration ;
- [#102](https://github.com/scalacenter/scalajs-bundler/pull/102): Make it possible to set the version of jsdom and webpack-dev-server to use ;
- [#106](https://github.com/scalacenter/scalajs-bundler/pull/106): Add a [Community](community.md) page ;
- [#108](https://github.com/scalacenter/scalajs-bundler/pull/108): Add gitter badge to the README ;
- [#121](https://github.com/scalacenter/scalajs-bundler/pull/121): Make the sbt task fail when webpack fails ;
- [#119](https://github.com/scalacenter/scalajs-bundler/pull/119): Add support for custom webpack config files in tests and in the reload workflow ;

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.5.0...v0.6.0).

## Version 0.5.0

> 2017 January 18

- [#57](https://github.com/scalacenter/scalajs-bundler/pull/57): Webpack 2.x support ;
- [#80](https://github.com/scalacenter/scalajs-bundler/pull/80): Upgrade to Scala.js 0.6.14 ;
- [#81](https://github.com/scalacenter/scalajs-bundler/pull/81): Disable the reload workflow by default ;
- [#94](https://github.com/scalacenter/scalajs-bundler/pull/94): Improve caching of tasks ;
- [#95](https://github.com/scalacenter/scalajs-bundler/pull/95): Fix support for spaces in paths ;

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.4.0...v0.5.0).

## Version 0.4.0

> 2016 December 15

- [#53](https://github.com/scalacenter/scalajs-bundler/pull/53): Fix cache invalidation when .js resources change ;
- [#54](https://github.com/scalacenter/scalajs-bundler/pull/54): Add support for importing assets from NPM packages ;
- [#56](https://github.com/scalacenter/scalajs-bundler/pull/56): Add [Yarn](https://yarnpkg.com/) support ;
- [#65](https://github.com/scalacenter/scalajs-bundler/pull/65): Use distinct target directories for `npmUpdate in Compile` and `npmUpdate in Test` ;
- [#69](https://github.com/scalacenter/scalajs-bundler/pull/69): Publish the API documentation ;
- [#70](https://github.com/scalacenter/scalajs-bundler/pull/70): Ensure that there is no duplicates in NPM dependencies ;
- [#71](https://github.com/scalacenter/scalajs-bundler/pull/71): Add a resolution mechanism for conflicting dependencies.

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.3.1...v0.4.0).

## Version 0.3.1

> 2016 December 2

- [#51](https://github.com/scalacenter/scalajs-bundler/pull/51): Support history API within jsdom.

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.3.0...v0.3.1).

## Version 0.3.0

> 2016 November 29

- [#32](https://github.com/scalacenter/scalajs-bundler/pull/32): Add a detailed documentation
section on how to write a facade with `@JSImport` (see it [here](cookbook.md#facade)) ;
- [#33](https://github.com/scalacenter/scalajs-bundler/pull/33): Fix cache invalidation
  after custom webpack config file is changed ;
- [#35](https://github.com/scalacenter/scalajs-bundler/pull/35): Fix tests on Windows ;
- [#37](https://github.com/scalacenter/scalajs-bundler/pull/37): Throw an error if there is no main class ;
- [#39](https://github.com/scalacenter/scalajs-bundler/pull/39): Add support for jsdom in tests ;
- [#45](https://github.com/scalacenter/scalajs-bundler/pull/45): Forbid `scalaJSModuleKind`
  to be different from `CommonJSModule` on projects where `ScalaJSBundler` plugin is enabled.

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.2.1...v0.3.0).

## Version 0.2.1

> 2016 November 2

- [#24](https://github.com/scalacenter/scalajs-bundler/pull/24): Fixed npm command in Windows (thanks
  to [@DylanArnold](https://github.com/DylanArnold)) ;
- [#25](https://github.com/scalacenter/scalajs-bundler/pull/25): Fixed the `scalaJSPipeline` task
  to correctly support source maps.

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.2...v0.2.1).

## Version 0.2

> 2016 November 1

- [#14](https://github.com/scalacenter/scalajs-bundler/pull/14): Improved documentation ;
- [#17](https://github.com/scalacenter/scalajs-bundler/pull/17): Faster live-reloading workflow
  (`fastOptJS::webpack` is ~10x faster) ;
- [#18](https://github.com/scalacenter/scalajs-bundler/pull/18): `ScalaJSBundlerPlugin` is not
  anymore automatically triggered: you have to manually enable it on your projects.
  `scalaJSModuleKind` is automatically set to `ModuleKind.CommonJSModule` when `ScalaJSBundlerPlugin`
  is enabled, so you don’t anymore have to set it in your build ;
- [#20](https://github.com/scalacenter/scalajs-bundler/pull/20): JavaScript files that are on
  the classpath can be `@JSImport`ed by your Scala facades.

You can find the complete list of commits since the last release
[here](https://github.com/scalacenter/scalajs-bundler/compare/v0.1...v0.2).
