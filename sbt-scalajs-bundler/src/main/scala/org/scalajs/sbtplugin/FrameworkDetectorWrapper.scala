package org.scalajs.sbtplugin

import org.scalajs.core.tools.linker.backend.ModuleKind
import org.scalajs.jsenv.JSEnv

// HACK Because FrameworkDetector is private
class FrameworkDetectorWrapper(jsEnv: JSEnv, moduleKind: ModuleKind, moduleIdentifier: Option[String]) {
  val wrapped = new FrameworkDetector(jsEnv, moduleKind, moduleIdentifier)
}
