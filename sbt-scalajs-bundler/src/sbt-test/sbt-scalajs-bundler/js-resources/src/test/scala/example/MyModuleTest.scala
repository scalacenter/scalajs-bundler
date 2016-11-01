package example

import org.scalatest.FreeSpec

class MyModuleTest extends FreeSpec {

  "MyModule" - {

    "has a someUuid field" in {
      assert(MyModule.someUuid != null)
    }

  }

}
