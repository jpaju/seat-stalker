package fi.jpaju
package restaurant

import zio.*
import zio.test.Assertion.*
import zio.test.*

object RestaurantModelSpec extends ZIOSpecDefault:
  override def spec = suite("RestaurantModelSpec")(
    suite("PersonCount")(
      test("cannot be lower than 1") {
        check(Gen.int(Int.MinValue, 0)) { n =>
          val result = PersonCount.make(n)
          assert(result.toEither)(isLeft(anything))
        }
      },
      test("can be constructed from positive non-zero number") {
        check(Gen.int(1, Int.MaxValue)) { n =>
          val result = PersonCount.make(n)
          assert(result.toEither)(isRight(equalTo(n)))
        }
      }
    ),
    suite("RestaurantId")(
      test("cannot be empty") {
        val result = RestaurantId.make("")
        assert(result.toEither)(isLeft(anything))
      },
      test("can be constructed from non-empty string") {
        check(Gen.asciiString.nonEmpty) { str =>
          val result = RestaurantId.make(str)
          assert(result.toEither)(isRight(equalTo(str)))
        }
      }
    )
  )
