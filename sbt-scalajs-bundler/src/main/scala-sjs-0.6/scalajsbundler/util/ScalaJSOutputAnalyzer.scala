package scalajsbundler.util

import org.scalajs.core.ir.Trees.JSNativeLoadSpec
import org.scalajs.core.tools.io.VirtualScalaJSIRFile
import org.scalajs.core.tools.linker._
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend}
import org.scalajs.sbtplugin.Loggers
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.ModuleKind
import sbt.Logger

object ScalaJSOutputAnalyzer {

  /**
    * @return The list of ES modules imported by a Scala.js project
    * @param linkingUnit The Scala.js linking unit
    */
  def importedModules(linkingUnit: LinkingUnit): List[String] =
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

  /**
    * Extract the linking unit from the Scala.js output
    *
    * @param linkerConfig Configuration of the Scala.js linker
    * @param linker Scala.js linker
    * @param irFiles Scala.js IR files
    * @param moduleInitializers Scala.js module initializers
    * @param logger Logger
    * @return
    */
  def linkingUnit(
      linkerConfig: StandardLinker.Config,
      linker: ClearableLinker,
      irFiles: Seq[VirtualScalaJSIRFile],
      moduleInitializers: Seq[ModuleInitializer],
      logger: Logger
  ): LinkingUnit = {
    require(linkerConfig.moduleKind != ModuleKind.NoModule,
            s"linkerConfig.moduleKind was ModuleKind.NoModule")
    val symbolRequirements = {
      val backend = new BasicLinkerBackend(linkerConfig.semantics,
                                           linkerConfig.esFeatures,
                                           linkerConfig.moduleKind,
                                           linkerConfig.sourceMap,
                                           LinkerBackend.Config())
      backend.symbolRequirements
    }
    linker.linkUnit(irFiles,
                    moduleInitializers,
                    symbolRequirements,
                    Loggers.sbtLogger2ToolsLogger(logger))
  }

}
