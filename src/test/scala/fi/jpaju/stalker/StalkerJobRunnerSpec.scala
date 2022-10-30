package fi.jpaju
package stalker

import fi.jpaju.restaurant.*
import fi.jpaju.telegram.*
import zio.*
import zio.stream.*
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
          sentMessages <- FakeTelegramService.getSentMessages
        yield assertTrue(sentMessages.isEmpty)
      }
    },
    test("should send notification when tables available") {
      val availableTablesGen = Gen.listOfBounded(1, 50)(Gens.availableTable)

      check(Gens.stalkerJobDefinition, availableTablesGen) { (jobDefinitions, availableTable) =>
        val availableTables = Map(jobDefinitions.restaurant.id -> ZStream.fromIterable(availableTable))

        for
          _            <- FakeTableService.setAvailableTables(availableTables)
          service      <- ZIO.service[StalkerJobRunner]
          _            <- service.runJob(jobDefinitions)
          sentMessages <- FakeTelegramService.getSentMessages <* FakeTelegramService.resetSentMessages
        yield assertTrue(sentMessages.size == 1)
      }
    },
    test("should notify only about the first ten available tables") {
      check(Gens.stalkerJobDefinition, Gens.availableTable) { (jobDefinition, availableTable) =>
        val infiniteAvailableTables = ZStream.repeat(availableTable)
        val availableTables         = Map(jobDefinition.restaurant.id -> infiniteAvailableTables)

        for
          _            <- FakeTableService.setAvailableTables(availableTables)
          service      <- ZIO.service[StalkerJobRunner]
          _            <- service.runJob(jobDefinition)
          sentMessages <- FakeTelegramService.getSentMessages

          // Definetely not the best way to test how many tables were sent, but works for now
          msgLineCount = sentMessages.headOption.map(_.split("\n").length).getOrElse(0)
          tolerance    = 3
        yield assertTrue(msgLineCount < 10 + tolerance)
      }
    }
  ).provide(
    LiveStalkerJobRunner.layer,
    FakeTelegramService.layer,
    FakeTableService.layer,
    Runtime.removeDefaultLoggers
  )
