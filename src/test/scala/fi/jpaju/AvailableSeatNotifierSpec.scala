package fi.jpaju

import zio.*
import zio.test.Assertion.*
import zio.test.*

import java.time.LocalDateTime

object AvailableSeatNotifierSpec extends ZIOSpecDefault:
  override def spec = suite("AvailableSeatNotifierSpec")(
    test("should not send notification when no seats are available") {
      check(Gens.seatRequirements) { params =>
        for
          service      <- ZIO.service[AvailableSeatNotifier]
          _            <- service.checkAndNotify(params)
          sentMessages <- getSentTelegramMessages
        yield assertTrue(sentMessages.isEmpty)
      }
    },
    test("should send notification when seats available") {
      val availableSeatsGen = Gen.listOfBounded(1, 50)(Gens.availableSeat)

      check(Gens.seatRequirements, availableSeatsGen) { (requirements, availableSeats) =>
        val seatStatus = SeatStatus.Available(availableSeats)

        for
          _            <- setAvailableSeats(Map(requirements.restaurant.id -> seatStatus))
          service      <- ZIO.service[AvailableSeatNotifier]
          _            <- service.checkAndNotify(requirements)
          sentMessages <- getSentTelegramMessages <* resetSentTelegramMessages
        yield assertTrue(sentMessages.size == 1)
      }
    }
  ).provide(
    LiveAvailableSeatNotifier.layer,
    FakeTelegramService.layer,
    FakeAvailableSeatsService.layer,
    ZLayer.fromZIO(Ref.make(List.empty[TelegramMessageBody])),
    ZLayer.fromZIO(Ref.make(Map.empty[RestaurantId, SeatStatus]))
  )

  private def getSentTelegramMessages: URIO[FakeTelegramService, List[TelegramMessageBody]] =
    ZIO.serviceWithZIO[FakeTelegramService](_.ref.get)

  private def resetSentTelegramMessages: URIO[FakeTelegramService, Unit] =
    ZIO.serviceWithZIO[FakeTelegramService](_.ref.set(List.empty))

  private def setAvailableSeats(statuses: Map[RestaurantId, SeatStatus]): URIO[FakeAvailableSeatsService, Unit] =
    ZIO.serviceWithZIO[FakeAvailableSeatsService](_.seats.set(statuses))
