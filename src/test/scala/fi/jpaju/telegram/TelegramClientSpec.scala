package fi.jpaju
package telegram

import sttp.client3.*
import sttp.client3.testing.*
import sttp.model.*
import zio.*
import zio.test.*

object TelegramClientSpec extends ZIOSpecDefault:
  override def spec = suite("TelegramClientSpec")(
    test("when sending message, then Telegram API is called with correct parameters") {
      check(Gens.telegramConfig, Gens.telegramMessageBody) { (telegramConfig, messageBody) =>
        val chatId = telegramConfig.chatId

        val recordingBackend = new RecordingSttpBackend(
          HttpClientZioBackend.stub.whenAnyRequest
            .thenRespond(responseJsonBody(chatId))
        )

        def assertCorrectRequest(request: Request[?, ?]): TestResult =
          val uri                 = request.uri
          val expectedQueryParams = Map[String, String](
            "chat_id" -> chatId.toString,
            "text"    -> messageBody.toString
          )

          assert(uri.host)(equalTo(Some("api.telegram.org"))) &&
          assert(uri.path)(equalTo(List(s"bot${telegramConfig.token}", "sendMessage"))) &&
          assert(uri.paramsMap)(equalTo(expectedQueryParams))
        end assertCorrectRequest

        withTelegramClient(telegramConfig, recordingBackend) { client =>
          for
            _      <- client.sendMessage(messageBody)
            request = recordingBackend.allInteractions.head._1
          yield assertCorrectRequest(request)
        }
      }
    },
    test("when sending message with bad telegram API token, then should fail") {
      val badTokenResponse = Response(
        s"""
        {
            "ok": false,
            "error_code": 404,
            "description": "Not Found"
        }""",
        StatusCode.NotFound
      )

      val sttpBackendStub = HttpClientZioBackend.stub.whenAnyRequest
        .thenRespond(badTokenResponse)

      check(Gens.telegramConfig, Gens.telegramMessageBody) { (telegramConfig, messageBody) =>
        withTelegramClient(telegramConfig, sttpBackendStub) { client =>
          client.sendMessage(messageBody).exit.map { exit =>
            assert(exit.isFailure)(isTrue)
          }
        }
      }
    },
    test("when sending message with invalid chat id, then should fail") {
      val chatNotFoundResponse = Response(
        s"""
        {
            "ok": false,
            "error_code": 400,
            "description": "Bad Request: chat not found"
        }""",
        StatusCode.BadRequest
      )

      val sttpBackendStub = HttpClientZioBackend.stub.whenAnyRequest
        .thenRespond(chatNotFoundResponse)

      check(Gens.telegramConfig, Gens.telegramMessageBody) { (telegramConfig, messageBody) =>
        withTelegramClient(telegramConfig, sttpBackendStub) { client =>
          val result = client.sendMessage(messageBody).exit
          assertZIO(result)(fails(anything))
        }
      }
    }
  ).provide(Runtime.removeDefaultLoggers)

  // =============================================== Helpers ===============================================

  private def responseJsonBody(@annotation.unused("used in tests") chatId: String): String = s"""
    {
        "ok": true,
        "result": {
            "message_id": 10,
            "from": {
                "id": 5792135341,
                "is_bot": true,
                "first_name": "Bot name",
                "username": "bot_name_bot"
            },
            "chat": {
                "id": -,
                "title": "Chat name",
                "type": "supergroup"
            },
            "date": 1665310148,
            "text": "Message text"
        }
    }
  """

  private def withTelegramClient[R, E, A](config: TelegramConfig, sttpBackend: SttpBackend[Task, Any])(
      f: TelegramClient => ZIO[R, E, A]
  ): ZIO[R, E, A] =
    val hardcodedConfig = Map(
      "TELEGRAM_TOKEN"  -> config.token,
      "TELEGRAM_CHATID" -> config.chatId
    )
    val configProvider  = ConfigProvider
      .fromMap(hardcodedConfig, pathDelim = "_")
      .upperCase

    ZIO
      .serviceWithZIO[TelegramClient](f)
      .provideSome[R](
        LiveTelegramClient.layer.orDie,
        ZLayer.succeed(sttpBackend)
      )
      .withConfigProvider(configProvider)
