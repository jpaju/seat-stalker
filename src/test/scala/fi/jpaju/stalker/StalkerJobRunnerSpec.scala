package fi.jpaju
package stalker

import fi.jpaju.restaurant.*
import fi.jpaju.telegram.*
import zio.*
import zio.test.Assertion.*
import zio.test.*

import java.time.LocalDateTime

object StalkerJobRunnerSpec extends ZIOSpecDefault:
  override def spec = suite("StalkerJobRunnerSpec")(
    test("should not send notification when no tables are available") {
      check(Gens.stalkerJobDefinition) { jobDefinition =>
        for
          service      <- ZIO.service[StalkerJobRunner]
          _            <- service.runJob(jobDefinition)
          sentMessages <- getSentTelegramMessages
        yield assertTrue(sentMessages.isEmpty)
      }
    },
    test("should send notification when tables available") {
      val availableTablesGen = Gen.listOfBounded(1, 50)(Gens.availableTable)

      check(Gens.stalkerJobDefinition, availableTablesGen) { (jobDefinitions, availableTable) =>
        val tableStatus = TableStatus.Available(jobDefinitions.restaurant, availableTable)

        for
          _            <- setAvailableTables(Map(jobDefinitions.restaurant.id -> tableStatus))
          service      <- ZIO.service[StalkerJobRunner]
          _            <- service.runJob(jobDefinitions)
          sentMessages <- getSentTelegramMessages <* resetSentTelegramMessages
        yield assertTrue(sentMessages.size == 1)
      }
    }
  ).provide(
    LiveStalkerJobRunner.layer,
    FakeTelegramService.layer,
    FakeTableService.layer
  )

  private def getSentTelegramMessages: URIO[FakeTelegramService, List[TelegramMessageBody]] =
    ZIO.serviceWithZIO[FakeTelegramService](_.ref.get)

  private def resetSentTelegramMessages: URIO[FakeTelegramService, Unit] =
    ZIO.serviceWithZIO[FakeTelegramService](_.ref.set(List.empty))

  private def setAvailableTables(statuses: Map[RestaurantId, TableStatus]): URIO[FakeTableService, Unit] =
    ZIO.serviceWithZIO[FakeTableService](_.tables.set(statuses))
