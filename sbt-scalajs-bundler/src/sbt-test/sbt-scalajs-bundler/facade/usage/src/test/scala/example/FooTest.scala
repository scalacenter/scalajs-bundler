package example

import org.scalatest.FreeSpec

class FooTest extends FreeSpec {

  "Foo" - {

    "has a bar method" in {
      assert(Foo.bar() != null)
    }

  }

}
