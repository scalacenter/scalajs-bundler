# Changelog

## Version 0.11.0

> 2018 January 31

- [#228](https://github.com/scalacenter/scalajs-bundler/pull/228): The npmUpdate method is accessible from a separate object;

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
