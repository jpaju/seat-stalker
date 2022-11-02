package fi.jpaju
package stalker

import fi.jpaju.restaurant.*
import fi.jpaju.telegram.*
import zio.*
import zio.stream.*

import java.time.LocalDateTime

object StalkerJobRunnerSpec extends ZIOSpecDefault:
  override def spec = suite("StalkerJobRunnerSpec")(
    test("should not send notification when no tables are available") {
      for
        service      <- ZIO.service[StalkerJobRunner]
        _            <- service.runJob(jobDefinition)
        sentMessages <- FakeTelegramService.getSentMessages
      yield assertTrue(sentMessages.isEmpty)
    },
    test("should send notification when tables available") {
      val availableTables = Map(jobDefinition.restaurant.id -> ZStream(availableTable))

      for
        _            <- FakeTableService.setAvailableTables(availableTables)
        service      <- ZIO.service[StalkerJobRunner]
        _            <- service.runJob(jobDefinition)
        sentMessages <- FakeTelegramService.getSentMessages <* FakeTelegramService.resetSentMessages
      yield assertTrue(sentMessages.size == 1)
    },
    test("should notify only about the first ten available tables") {
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
    },
    test("running job should die if checking available tables fails") {
      val availableTables = Map(jobDefinition.restaurant.id -> ZStream.dieMessage("Checking tables failed"))

      for
        _      <- FakeTableService.setAvailableTables(availableTables)
        runner <- ZIO.service[StalkerJobRunner]
        result <- runner.runJob(jobDefinition).exit
      yield assert(result)(dies(anything))
    },
    test("running job should die if sending notification fails") {
      val availableTables = Map(jobDefinition.restaurant.id -> ZStream(availableTable))

      val deliveryError = MessageDeliveryError("Sending failed", new RuntimeException("Sending failed"))

      for
        _      <- FakeTableService.setAvailableTables(availableTables)
        _      <- FakeTelegramService.setSendMessageFunction(_ => ZIO.fail(deliveryError))
        runner <- ZIO.service[StalkerJobRunner]
        result <- runner.runJob(jobDefinition).exit
      yield assert(result)(dies(anything))
    }
  ).provide(
    LiveStalkerJobRunner.layer,
    FakeTelegramService.layer,
    FakeTableService.layer,
    Runtime.removeDefaultLoggers
  )

  private def jobDefinition: StalkerJobDefinition =
    StalkerJobDefinition(
      Restaurant(RestaurantId("34"), "Test restaurant"),
      PersonCount(2)
    )

  private def availableTable: AvailableTable =
    AvailableTable(LocalDateTime.parse("2022-11-02T10:15:30"), PersonCount(2))
