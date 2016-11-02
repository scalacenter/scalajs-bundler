# Changelog

## Version 0.2.1

> 2016 November 2

- [#24](https://github.com/scalacenter/scalajs-bundler/pull/24): Fixed npm command in Windows (thanks
  to [@DylanArnold](https://github.com/DylanArnold)) ;
- [#25](https://github.com/scalacenter/scalajs-bundler/pull/25): Fixed the `scalaJSPipeline` task
  to correctly support source maps.

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
