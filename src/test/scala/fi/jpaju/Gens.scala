package fi.jpaju

import zio.*
import zio.prelude.*
import zio.test.*

object Gens:

  // ============================================== Restaurant ==============================================

  val restaurantId: Gen[Any, RestaurantId] =
    Gen.string.nonEmpty
      .map(RestaurantId.make(_))
      .collectSuccess

  val restaurant: Gen[Any, Restaurant] =
    (restaurantId <*> Gen.string.nonEmpty).map(Restaurant.apply)

  val seatCount: Gen[Any, SeatCount] = Gen
    .int(1, 100)
    .map(SeatCount.make(_))
    .collectSuccess

  val availableSeat: Gen[Any, AvailableSeat] =
    (Gen.localDateTime <*> seatCount).map(AvailableSeat.apply)

  // =============================================== Services ===============================================

  val seatRequirements: Gen[Any, SeatRequirements] =
    (restaurant <*> seatCount).map(SeatRequirements.apply)

  val checkSeatParameters: Gen[Any, CheckSeatsParameters] = (
    restaurantId <*> Gen.localDate <*> seatCount
  ).map(CheckSeatsParameters(_, _, _))

  // =============================================== Telegram ===============================================

  val telegramConfig: Gen[Any, TelegramConfig] =
    (Gen.asciiString <*> Gen.long).map(TelegramConfig.apply)

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
