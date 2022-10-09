package fi.jpaju

import zio.*
import zio.prelude.Validation
import zio.test.Assertion.*
import zio.test.*

object TelegramDomainSpec extends ZIOSpecDefault:
  override def spec = suite("TelegramDomainSpec")(
    suite("TelegramMessageBody")(
      test("cannot be empty") {
        val result: Validation[String, TelegramMessageBody.Type] = TelegramMessageBody.make("")
        assert(result.toEither)(isLeft(anything))
      },
      test("can be constructed from non-empty string") {
        check(Gen.stringBounded(1, 100)(Gen.unicodeChar)) { str =>
          val result = TelegramMessageBody.make(str)
          assert(result.toEither)(isRight(equalTo(str)))
        }
      }
    )
  )
