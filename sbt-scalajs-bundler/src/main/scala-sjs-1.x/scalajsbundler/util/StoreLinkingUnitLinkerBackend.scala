package org.scalajs.linker.scalajsbundler

import org.scalajs.linker.interface._
import org.scalajs.linker.standard._

import org.scalajs.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

class StoreLinkingUnitLinkerBackend(linkerConfig: StandardConfig) extends LinkerBackend  {
  private val underlyingBackend: LinkerBackend =
    StandardLinkerBackend(linkerConfig)

  val coreSpec: CoreSpec = underlyingBackend.coreSpec

  val symbolRequirements: SymbolRequirement = underlyingBackend.symbolRequirements

  def injectedIRFiles: Seq[IRFile] = underlyingBackend.injectedIRFiles

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
