package scalajsbundler.util

import org.scalajs.core.ir.Trees.JSNativeLoadSpec
import org.scalajs.core.tools.io.WritableVirtualJSFile
import org.scalajs.core.tools.javascript.ESLevel
import org.scalajs.core.tools.linker.{ModuleKind => _, _}
import org.scalajs.core.tools.linker.analyzer._
import org.scalajs.core.tools.linker.backend._
import org.scalajs.core.tools.linker.frontend._
import org.scalajs.core.tools.logging.Logger

import scalajsbundler.WebpackEntryPoint

import java.io.File

final class EntryPointAnalyzerBackend(
  semantics: Semantics,
  esFeatures: OutputMode,
  moduleKind: ModuleKind,
  withSourceMap: Boolean,
  config: LinkerBackend.Config,
  entryPoint: File
) extends LinkerBackend(semantics, esFeatures.esLevel, moduleKind, withSourceMap, config) {

  require(moduleKind == ModuleKind.CommonJSModule, s"moduleKind was $moduleKind")

  private val standard = new BasicLinkerBackend(semantics, esFeatures, moduleKind, withSourceMap, config)

  val symbolRequirements: SymbolRequirement = standard.symbolRequirements

  def emit(unit: LinkingUnit, output: WritableVirtualJSFile, logger: Logger): Unit = {
    WebpackEntryPoint.writeEntryPoint(importedModules(unit), entryPoint)
    standard.emit(unit, output, logger)
  }

  private def importedModules(linkingUnit: LinkingUnit): List[String] = {
    linkingUnit.classDefs
      .flatMap(_.jsNativeLoadSpec)
      .flatMap {
        case JSNativeLoadSpec.Import(module, _) => List(module)
        case JSNativeLoadSpec.ImportWithGlobalFallback(
            JSNativeLoadSpec.Import(module, _),
            _) =>
          List(module)
        case JSNativeLoadSpec.Global(_) => Nil
      }
      .distinct
  }
}
