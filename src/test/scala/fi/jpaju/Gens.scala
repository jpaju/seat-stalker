package fi.jpaju

import zio.*
import zio.test.*

object Gens:
  val telegramConfig: Gen[Any, TelegramConfig] =
    (Gen.asciiString <*> Gen.int).map(TelegramConfig.apply)

  val telegramMessageBody: Gen[Any, TelegramMessageBody] =
    Gen // Use large generator to make sure long messages are generated
      .stringBounded(1, 100)(Gen.unicodeChar)
      .map(str => TelegramMessageBody.make(str).toEither)
      .collect { case Right(msg) => msg }
