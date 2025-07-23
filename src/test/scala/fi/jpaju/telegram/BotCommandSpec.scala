package fi.jpaju.telegram

import zio.*
import zio.test.*

object BotCommandSpec extends ZIOSpecDefault:
  def spec = suite("BotCommand")(
    test("should parse /echo command with message from telegram webhook payload") {
      val payload = createTelegramPayload("/echo Hello world!")
      val result  = BotCommand.fromUpdateJson(payload)

      assertTrue(result == Some(BotCommand.Echo("Hello world!")))
    },
    test("should parse /echo command without message from telegram webhook payload") {
      val payload = createTelegramPayload("/echo")
      val result  = BotCommand.fromUpdateJson(payload)

      assertTrue(result == Some(BotCommand.Echo("")))
    },
    test("should parse /jobs command from telegram webhook payload") {
      val payload = createTelegramPayload("/jobs")
      val result  = BotCommand.fromUpdateJson(payload)

      assertTrue(result == Some(BotCommand.ListJobs))
    }
  )

  private def createTelegramPayload(text: String): String = s"""{
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
      "text": "$text",
      "entities": [{ "offset": 0, "length": ${text.split(" ").head.length}, "type": "bot_command" }]
    }
  }"""
