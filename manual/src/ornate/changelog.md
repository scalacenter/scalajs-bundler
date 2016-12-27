# Changelog

## Version 0.5.0

> 2016 December 27

- [#81](https://github.com/scalacenter/scalajs-bundler/pull/81): Set default value of the setting `enableReloadWorkflow` to false ;

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
