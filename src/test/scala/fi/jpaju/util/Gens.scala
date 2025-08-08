package fi.jpaju
package util

import fi.jpaju.restaurant.*
import fi.jpaju.stalker.*
import fi.jpaju.telegram.*
import zio.*
import zio.prelude.*
import zio.test.*

object Gens:

  // ============================================== Restaurant ==============================================

  val restaurantId: Gen[Any, RestaurantId] =
    Gen.alphaNumericString.nonEmpty
      .map(RestaurantId.make(_))
      .collectSuccess

  val restaurant: Gen[Any, Restaurant] =
    (restaurantId <*> Gen.string.nonEmpty).map(Restaurant.apply)

  val personCount: Gen[Any, PersonCount] = Gen
    .int(1, 100)
    .map(PersonCount.make(_))
    .collectSuccess

  val availableTable: Gen[Any, AvailableTable] =
    (Gen.localDateTime <*> personCount).map(AvailableTable.apply)

  // =============================================== Services ===============================================

  val stalkerJobDefinition: Gen[Any, StalkerJobDefinition] =
    (restaurant <*> personCount).map(StalkerJobDefinition.apply)

  val checkTableParameters: Gen[Any, CheckTablesParameters] = (
    restaurant <*> personCount <*> Gen.localDateTime
  ).map(CheckTablesParameters(_, _, _))

  // =============================================== Telegram ===============================================

  val telegramConfig: Gen[Any, TelegramConfig] =
    (Gen.asciiString <*> Gen.asciiString.nonEmpty <*> Gen.asciiString.nonEmpty).map(TelegramConfig.apply)

  val telegramMessageBody: Gen[Any, TelegramMessageBody] =
    Gen // Use large generator to make sure long messages are generated
      .stringBounded(1, 100)(Gen.unicodeChar)
      .map(str => TelegramMessageBody.make(str).toEither)
      .collect { case Right(msg) => msg }

// ============================================== Utilities ==============================================

extension [R](gen: Gen[R, String])
  def nonEmpty: Gen[R, String] =
    gen.filter(_.nonEmpty)

extension [R, A](gen: Gen[R, Validation[?, A]])
  def collectSuccess: Gen[R, A] =
    gen.map(_.toEither).collect { case Right(a) => a }
