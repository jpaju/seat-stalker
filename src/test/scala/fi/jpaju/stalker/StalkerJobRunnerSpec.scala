package fi.jpaju
package stalker

import fi.jpaju.seating.*
import fi.jpaju.telegram.*
import zio.*
import zio.test.Assertion.*
import zio.test.*

import java.time.LocalDateTime

object StalkerJobRunnerSpec extends ZIOSpecDefault:
  override def spec = suite("StalkerJobRunnerSpec")(
    test("should not send notification when no seats are available") {
      check(Gens.stalkerJobDefinition) { jobDefinition =>
        for
          service      <- ZIO.service[StalkerJobRunner]
          _            <- service.runJob(jobDefinition)
          sentMessages <- getSentTelegramMessages
        yield assertTrue(sentMessages.isEmpty)
      }
    },
    test("should send notification when seats available") {
      val availableSeatsGen = Gen.listOfBounded(1, 50)(Gens.availableSeat)

      check(Gens.stalkerJobDefinition, availableSeatsGen) { (jobDefinitions, availableSeats) =>
        val seatStatus = SeatStatus.Available(availableSeats)

        for
          _            <- setAvailableSeats(Map(jobDefinitions.restaurant.id -> seatStatus))
          service      <- ZIO.service[StalkerJobRunner]
          _            <- service.runJob(jobDefinitions)
          sentMessages <- getSentTelegramMessages <* resetSentTelegramMessages
        yield assertTrue(sentMessages.size == 1)
      }
    }
  ).provide(
    LiveStalkerJobRunner.layer,
    FakeTelegramService.layer,
    FakeAvailableSeatsService.layer
  )

  private def getSentTelegramMessages: URIO[FakeTelegramService, List[TelegramMessageBody]] =
    ZIO.serviceWithZIO[FakeTelegramService](_.ref.get)

  private def resetSentTelegramMessages: URIO[FakeTelegramService, Unit] =
    ZIO.serviceWithZIO[FakeTelegramService](_.ref.set(List.empty))

  private def setAvailableSeats(statuses: Map[RestaurantId, SeatStatus]): URIO[FakeAvailableSeatsService, Unit] =
    ZIO.serviceWithZIO[FakeAvailableSeatsService](_.seats.set(statuses))
