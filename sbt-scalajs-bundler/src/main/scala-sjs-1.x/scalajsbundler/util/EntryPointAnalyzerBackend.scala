package scalajsbundler.util

import org.scalajs.ir.Trees.JSNativeLoadSpec
import org.scalajs.linker._
import org.scalajs.linker.standard._
import org.scalajs.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

import scalajsbundler.WebpackEntryPoint

import java.io.File

final class EntryPointAnalyzerBackend(linkerConfig: StandardLinker.Config, entryPoint: File) extends LinkerBackend  {
  require(linkerConfig.moduleKind == ModuleKind.CommonJSModule,
    s"linkerConfig.moduleKind was ${linkerConfig.moduleKind}")

  private val standard = StandardLinkerBackend(linkerConfig)

  val coreSpec: CoreSpec = standard.coreSpec
  val symbolRequirements: SymbolRequirement = standard.symbolRequirements

  def emit(unit: LinkingUnit, output: LinkerOutput, logger: Logger)(implicit ec: ExecutionContext): Future[Unit] = {
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
        case JSNativeLoadSpec.Global(_, _) => Nil
      }
      .distinct
  }
}
