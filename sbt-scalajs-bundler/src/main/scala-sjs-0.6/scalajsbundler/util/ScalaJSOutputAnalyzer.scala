package scalajsbundler.util

import org.scalajs.core.tools.linker._
import org.scalajs.core.tools.linker.frontend._
import org.scalajs.core.tools.linker.frontend.optimizer._
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend}

import java.io.File

object ScalaJSOutputAnalyzer {
  def linker(config: StandardLinker.Config, entryFile: File): ClearableLinker = {
    require(!config.closureCompiler)

    val frontendConfig = LinkerFrontend.Config()
      .withBypassLinkingErrors(config.bypassLinkingErrors)
      .withCheckIR(config.checkIR)

    val backendConfig = LinkerBackend.Config()
      .withRelativizeSourceMapBase(config.relativizeSourceMapBase)
      .withCustomOutputWrapper(config.customOutputWrapper)
      .withPrettyPrint(config.prettyPrint)

    val optOptimizerFactory = {
      if (!config.optimizer) None
      else if (config.parallel) Some(ParIncOptimizer.factory)
      else Some(IncOptimizer.factory)
    }

    new ClearableLinker(() => {
      val frontend = new LinkerFrontend(config.semantics, config.esFeatures.esLevel,
        config.sourceMap, frontendConfig, optOptimizerFactory)
      val backend = new EntryPointAnalyzerBackend(config.semantics, config.esFeatures,
          config.moduleKind, config.sourceMap, backendConfig, entryFile)
      new Linker(frontend, backend)
    }, config.batchMode)
  }
}
