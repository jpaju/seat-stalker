package fi.jpaju

import sttp.client3.*
import sttp.client3.testing.*
import sttp.model.*
import zio.*
import zio.test.*

import java.time.*

object TelegramServiceSpec extends ZIOSpecDefault:
  override def spec = suite("TelegramServiceSpec")(
    test("when sending message, then Telegram API is called with correct parameters") {
      check(Gens.telegramConfig, Gens.telegramMessageBody) { (telegramConfig, messageBody) =>
        val chatId = telegramConfig.chatId

        val recordingBackend = new RecordingSttpBackend(
          AsyncHttpClientZioBackend.stub.whenAnyRequest
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

        withTelegramService(telegramConfig, recordingBackend) { service =>
          for
            _      <- service.sendMessage(messageBody)
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

      val sttpBackendStub = AsyncHttpClientZioBackend.stub.whenAnyRequest
        .thenRespond(badTokenResponse)

      check(Gens.telegramConfig, Gens.telegramMessageBody) { (telegramConfig, messageBody) =>
        withTelegramService(telegramConfig, sttpBackendStub) { service =>
          service.sendMessage(messageBody).exit.map { exit =>
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

      val sttpBackendStub = AsyncHttpClientZioBackend.stub.whenAnyRequest
        .thenRespond(chatNotFoundResponse)

      check(Gens.telegramConfig, Gens.telegramMessageBody) { (telegramConfig, messageBody) =>
        withTelegramService(telegramConfig, sttpBackendStub) { service =>
          val result = service.sendMessage(messageBody).exit
          assertZIO(result)(fails(anything))
        }
      }
    }
  ).provide(Runtime.removeDefaultLoggers)

  // =============================================== Helpers ===============================================

  private def responseJsonBody(chatId: Long): String = s"""
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

  private def withTelegramService[R, E, A](config: TelegramConfig, sttpBackend: SttpBackend[Task, Any])(
      f: TelegramService => ZIO[R & TelegramService, E, A]
  ): ZIO[R, E, A] =
    ZIO
      .serviceWithZIO[TelegramService](f)
      .provideSome[R](
        LiveTelegramService.layer,
        ZLayer.succeed(config),
        ZLayer.succeed(sttpBackend)
      )
