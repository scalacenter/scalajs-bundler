package example

import org.junit.Assert._
import org.junit.Test

class FooTest {

  @Test def bar(): Unit = {
    assertNotNull(Foo.bar())
  }

}
