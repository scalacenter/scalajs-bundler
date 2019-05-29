package scalajsbundler.util

import org.scalajs.linker._
import org.scalajs.linker.standard._

import java.io.File

object ScalaJSOutputAnalyzer {
  def linker(config: StandardLinker.Config, entryPoint: File): ClearableLinker = {
    ClearableLinker(() => {
      val frontend = StandardLinkerFrontend(config)
      val backend = new EntryPointAnalyzerBackend(config, entryPoint)
      StandardLinkerImpl(frontend, backend)
    }, config.batchMode)
  }
}
