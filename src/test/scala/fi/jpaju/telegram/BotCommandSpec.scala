package fi.jpaju.telegram

import zio.*
import zio.test.*

object BotCommandSpec extends ZIOSpecDefault:
  def spec = suite("BotCommand")(
    test("should parse /echo command with message") {
      val result = BotCommand.fromText("/echo Hello world!")
      assertTrue(result == Some(BotCommand.Echo("Hello world!")))
    },
    test("should parse /echo command without message from telegram webhook payload") {
      val result = BotCommand.fromText("/echo")
      assertTrue(result == Some(BotCommand.Echo("")))
    },
    test("should parse /jobs command from telegram webhook payload") {
      val result = BotCommand.fromText("/jobs")
      assertTrue(result == Some(BotCommand.ListJobs))
    }
  )
