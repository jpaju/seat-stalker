package fi.jpaju

import fi.jpaju.restaurant.*
import fi.jpaju.telegram.*
import fi.jpaju.stalker.*
import fi.jpaju.util.*

import zio.*
import zio.stream.*
import zio.test.*

import java.time.*

object IntegrationTestSpec extends ZIOSpecDefault:
  override def spec = suite("IntegrationTestSpec")(
    // TODO Use testcontainers/mockserver in order to send actual HTTP requests (TelegramClient and TableService)
    // TODO Run the app by calling the SeatStalkerTimerFunction.run method
    test("Sends telegram message about available tables") {
      val jobDefinition   = StalkerJobDefinition(Restaurant(RestaurantId("34"), "The restaurant"), PersonCount(2))
      val availableTable  = AvailableTable(LocalDateTime.parse("2022-11-02T10:15:30"), PersonCount(2))
      val availableTables = Map(jobDefinition.restaurant.id -> ZStream(availableTable))

      val expectedMessage = toExpectedMessage(jobDefinition.restaurant, availableTable)

      for
        _            <- ZIO.serviceWithZIO[StalkerJobRepository](_.saveJob(jobDefinition))
        _            <- FakeTableService.setAvailableTables(availableTables)
        _            <- ZIO.serviceWithZIO[StalkerApp](_.run)
        sentMessages <- FakeTelegramClient.getSentMessages
      yield assertTrue(sentMessages.map(_.toString) == List(expectedMessage))
    }
  ).provide(
    LiveStalkerApp.layer,
    LiveStalkerJobRunner.layer,
    DefaultMessageFormatter.layer,
    InMemoryStalkerJobRepository.layerFromJobs(Set.empty),
    FakeTelegramClient.layer,
    FakeTableService.layer,
    Runtime.removeDefaultLoggers
  )

  def toExpectedMessage(restaurant: Restaurant, availableTable: AvailableTable) =
    DefaultMessageFormatter.tablesAvailableMessage(restaurant, List(availableTable))
