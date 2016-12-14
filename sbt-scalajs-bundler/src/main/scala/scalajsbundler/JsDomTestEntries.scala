package scalajsbundler

import sbt._

import scalajsbundler.util.JS

object JsDomTestEntries {

  /**
    * Loads the output of Scala.js and exports all its exported properties to the global namespace,
    * so that they are found by jsdom.
    * @param sjsOutput Scala.js output
    * @param loaderFile File to write the loader to
    */
  def writeLoader(sjsOutput: File, loaderFile: File): Unit = {
    val window = JS.ref("window")
    val require = JS.ref("require")
    val Object = JS.ref("Object")
    val loader =
      JS.let(
        require.apply(JS.str(sjsOutput.absolutePath))
      ) { tests =>
        Object.dot("keys").apply(tests).dot("forEach").apply(JS.fun { key =>
          window.bracket(key).assign(tests.bracket(key)) // Export all properties of the Scala.js module to the global namespace
        })
      }

    IO.write(loaderFile, loader.show)
  }

}
