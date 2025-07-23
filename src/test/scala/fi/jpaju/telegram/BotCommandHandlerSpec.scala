package fi.jpaju.telegram

import java.time.Instant
import fi.jpaju.stalker.*
import fi.jpaju.restaurant.*
import fi.jpaju.util.FakeTelegramClient
import zio.*
import zio.test.*

object BotCommandHandlerSpec extends ZIOSpecDefault:
  override def spec = suite("BotCommandHandler")(
    test("should send echo message when handling Echo command") {
      for
        // Given
        telegramClient <- ZIO.service[FakeTelegramClient]
        jobRepository  <- ZIO.service[StalkerJobRepository]
        handler         = LiveBotCommandHandler(telegramClient, jobRepository)
        msg             = "Hello world!"

        // When
        _ <- handler.handle(BotCommand.Echo(msg), randomContext())

        // Then
        sentMessages <- FakeTelegramClient.getSentMessages
      yield assertTrue(sentMessages.head.contains(msg))
    },
    test("should send jobs list when handling ListJobs command with empty repository") {
      for
        // Given
        telegramClient <- ZIO.service[FakeTelegramClient]
        jobRepository  <- ZIO.service[StalkerJobRepository]
        handler         = LiveBotCommandHandler(telegramClient, jobRepository)

        // When
        _ <- handler.handle(BotCommand.ListJobs, randomContext())

        // Then
        sentMessages <- FakeTelegramClient.getSentMessages
      yield assertTrue(sentMessages.head == "No active jobs currently.")
    },
    test("should send formatted jobs list when handling ListJobs command with existing jobs") {
      for
        // Given
        telegramClient <- ZIO.service[FakeTelegramClient]
        jobRepository  <- ZIO.service[StalkerJobRepository]
        foo             = Restaurant(RestaurantId("123"), "Foo")
        bar             = Restaurant(RestaurantId("456"), "Bar")
        jobs            = Set(
                            StalkerJobDefinition(foo, PersonCount(2)),
                            StalkerJobDefinition(bar, PersonCount(4))
                          )
        _              <- ZIO.foreach(jobs)(jobRepository.saveJob(_))
        handler         = LiveBotCommandHandler(telegramClient, jobRepository)

        // When
        _ <- handler.handle(BotCommand.ListJobs, randomContext())

        // Then
        sentMessages <- FakeTelegramClient.getSentMessages
      yield assertTrue(
        sentMessages.head.contains("Foo - 2 persons"),
        sentMessages.head.contains("Bar - 4 persons")
      )
    }
  ).provide(
    FakeTelegramClient.layer,
    InMemoryStalkerJobRepository.layerFromJobs(Set.empty)
  )

  private def randomContext(): MessageContext =
    MessageContext(ChatId("1234"), Instant.now())
