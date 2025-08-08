package fi.jpaju.telegram

import fi.jpaju.stalker.*
import fi.jpaju.util.FakeTelegramClient
import zio.*
import zio.test.*

object BotControllerSpec extends ZIOSpecDefault:
  override def spec = suite("BotController")(
    test("should parse JSON and delegate to handler for echo command") {
      for
        // Given
        controller <- createBotController

        // When
        _ <- controller.handleWebhook(echoCommandPayload, validHeaders)

        // Then
        Vector(sentMessage) <- FakeTelegramClient.getSentMessages
      yield assertTrue(sentMessage.contains("Hello from controller!"))
    },
    test("should reject webhook with invalid secret token") {
      for
        // Given
        controller <- createBotController
        headers     = Map("x-telegram-bot-api-secret-token" -> "invalid-token")

        // When
        result <- controller.handleWebhook(emptyPayload, headers).exit

      // Then
      yield assertTrue(result.is(_.failure).isInstanceOf[BotError.AuthenticationError])
    },
    test("should reject webhook without secret token") {
      for
        // Given
        controller  <- createBotController
        emptyHeaders = Map.empty[String, String]

        // When
        result <- controller.handleWebhook(emptyPayload, emptyHeaders).exit

      // Then
      yield assertTrue(result.is(_.failure).isInstanceOf[BotError.AuthenticationError])
    }
  ).provide(
    FakeTelegramClient.layer,
    LiveBotCommandHandler.layer,
    InMemoryStalkerJobRepository.layerFromJobs(Set.empty)
  )

  private def createBotController: ZIO[BotCommandHandler, Nothing, LiveBotController] =
    for
      handler   <- ZIO.service[BotCommandHandler]
      testConfig = TelegramConfig(botToken = "test-token", chatId = "12345", secretToken = "valid-secret")
    yield LiveBotController(handler, testConfig)

  private def validHeaders =
    Map("x-telegram-bot-api-secret-token" -> "valid-secret")

  private def emptyPayload = """{}"""

  private def echoCommandPayload = """{
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
