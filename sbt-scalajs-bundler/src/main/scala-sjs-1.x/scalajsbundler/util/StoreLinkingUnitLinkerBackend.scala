package org.scalajs.linker.scalajsbundler

import org.scalajs.linker._
import org.scalajs.linker.standard._
import org.scalajs.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

class StoreLinkingUnitLinkerBackend(linkerConfig: StandardLinker.Config) extends LinkerBackend  {
  val coreSpec: CoreSpec = linkerConfig.commonPhaseConfig.coreSpec
  val symbolRequirements: SymbolRequirement = {
    val backend = StandardLinkerBackend(linkerConfig)
    backend.symbolRequirements
  }

  @volatile
  private var _outputLinkingUnit: LinkingUnit = _

  def outputLinkingUnit: LinkingUnit = {
    if (_outputLinkingUnit == null)
      throw new IllegalStateException("must call link first")
    _outputLinkingUnit
  }

  def emit(unit: LinkingUnit, output: LinkerOutput, logger: Logger)(implicit ec: ExecutionContext): Future[Unit] = {
    _outputLinkingUnit = unit
    Future.successful(())
  }
}
