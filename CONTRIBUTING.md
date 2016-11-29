Contributing
===========

## Organization of the repository

- `sbt-scalajs-bundler/` The scalajs-bundler sbt plugin
- `sbt-web-scalajs-bundler/` sbt plugin for integrating scalajs-bundler and
[sbt-web-scalajs](https://github.com/vmunier/sbt-web-scalajs)
- `doc/` Sources of the documentation

## Building

~~~ sh
$ sbt package
~~~

## Running the tests

~~~ sh
$ sbt scripted
~~~

## Preview the documentation

~~~ sh
$ sbt doc/previewSite
~~~

## General recommendations

Test and document each added feature.