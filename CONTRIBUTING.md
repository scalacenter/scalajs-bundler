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

We use [sbt-scripted](http://eed3si9n.com/testing-sbt-plugins) to test the plugins.

To run all the tests:

~~~ sh
$ sbt runScripted
~~~

Or, to run a single test:

~~~ sh
$ sbt "sbt-scalajs-bundler/scripted sbt-scalajs-bundler/test-name"
~~~

(where `test-name` is replaced by one of the
[tests](https://github.com/scalacenter/scalajs-bundler/tree/master/sbt-scalajs-bundler/src/sbt-test/sbt-scalajs-bundler)).

Sometimes you would like to open an interactive sbt shell and manually play with
sbt tasks instead of writing them into a sbt-scripted test. In such a case, you
can start with an existing sbt-scripted test and add the following commands at
the top of its `test` file:

~~~
$ pause
$ fail
~~~

Then, when you will run the scripted task on this this test, it will start by
making a pause. You can then open a new sbt shell in the running test:

~~~
$ cd /tmp/sbt_fa1e13d43/test-name
$ sbt -Dplugin.version=x.y.z-SNAPSHOT
~~~

(where `x.y.z` is replaced by the current version of sbt-scalajs-bundler)

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
