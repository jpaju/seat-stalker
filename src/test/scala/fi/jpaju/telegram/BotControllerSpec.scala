package fi.jpaju.telegram

import fi.jpaju.stalker.*
import fi.jpaju.util.FakeTelegramClient
import zio.*
import zio.test.*

object BotControllerSpec extends ZIOSpecDefault:
  override def spec = suite("BotController")(
    test("should parse JSON and delegate to handler for echo command") {
      val echoCommandPayload = """{
        "update_id": 900828836,
        "message": {
          "message_id": 6255,
          "from": {
            "id": 104663478,
            "is_bot": false,
            "first_name": "Test",
            "last_name": "User",
            "language_code": "en"
          },
          "chat": {
            "id": 12345,
            "title": "Test Chat",
            "type": "supergroup"
          },
          "date": 1753271950,
          "text": "/echo Hello from controller!",
          "entities": [{ "offset": 0, "length": 5, "type": "bot_command" }]
        }
      }"""

      for
        // Given
        telegramClient <- ZIO.service[FakeTelegramClient]
        jobRepository  <- ZIO.service[StalkerJobRepository]
        handler         = LiveBotCommandHandler(telegramClient, jobRepository)
        controller      = LiveBotController(handler)

        // When
        _ <- controller.handleWebhook(echoCommandPayload)

        // Then
        sentMessages <- FakeTelegramClient.getSentMessages
      yield assertTrue(sentMessages.head.contains("Hello from controller!"))
    }
  ).provide(
    FakeTelegramClient.layer,
    InMemoryStalkerJobRepository.layerFromJobs(Set.empty)
  )
