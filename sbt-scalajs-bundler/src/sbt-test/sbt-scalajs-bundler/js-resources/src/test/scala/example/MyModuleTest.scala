package example

import org.junit.Assert._
import org.junit.Test

class MyModuleTest {

  @Test def someUuid(): Unit = {
    assertNotNull(MyModule.someUuid)
  }

}
