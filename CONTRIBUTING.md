Contributing
===========

## Organization of the repository

- `sbt-scalajs-bundler/` The scalajs-bundler sbt plugin
- `sbt-web-scalajs-bundler/` sbt plugin for integrating scalajs-bundler and
[sbt-web-scalajs](https://github.com/vmunier/sbt-web-scalajs)
- `manual/` Sources of the documentation

## Build the project

~~~ sh
$ sbt package
~~~

## Run the tests

~~~ sh
$ sbt scripted
~~~

## Publish locally

~~~ sh
$ sbt publishLocal
~~~

## Preview the documentation

~~~ sh
$ sbt manual/previewSite
~~~

## General recommendations

Test and document each added feature.