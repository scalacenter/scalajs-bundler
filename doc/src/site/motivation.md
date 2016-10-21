# Motivation

The most popular JavaScript package registry is npm. Scala.js projects are usually based on sbt,
which has no knowledge of npm. So, how can Scala.js developers get an *integrated experience* when
they use libraries published on npm?

## WebJars

A first solution is to use [WebJars](http://www.webjars.org/). WebJars wrap .js files into .jar
archives published on maven central, so that Scala developers can depend on them just like
they depend on JVM libraries. However, WebJars have limitations.

First, npm packages are not automatically available as WebJars as soon as they are published on
the npm registry. So, WebJars users have to explicitly publish these npm packages as .jar packages.
This is just a matter of filling and submitting a form with the name and version of the package, 
but, still, this is one extra step.

Second, the tool that re-publishes npm packages as .jar packages does not keep track of
[transitive dependencies](https://github.com/webjars/webjars/issues/1186).
It means that when your program depends on a library `foo` that itself depends on a library `bar`,
then you have to explicitly convert both and depend on both in your program. This situation
might turn into a dependency management hell.

## Double build

Another solution consists in having two build systems: one for the Scala world and one for
the npm world. At some point, the npm build writes files consumed by the Scala application.

Typically, the npm build defines npm dependencies and bundles them into a single .js
file suitable for web browser consumption and that the Scala.js program can depend
on (e.g. using `jsDependencies`).

This approach works well and allows developers to use whatever tools they want to process
the npm dependencies.

However, having two build systems is not really an _integrated_ developer experience:
developers have to setup two builds, run two shells, and manually take care of the
relationship between the two builds.

## scalajs-bundler

scalajs-bundler aims to provide an integrated solution to work with npm packages from
Scala.js projects. It lets developers define their npm dependencies from within their sbt build,
fetches them (using npm itself), and bundles them with the output of the Scala.js compilation.
The result is a single .js file suitable for web browser consumption.
