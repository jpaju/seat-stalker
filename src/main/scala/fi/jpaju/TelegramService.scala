package fi.jpaju

import sttp.client3.*
import sttp.client3.ziojson.*
import zio.*
import zio.json.*
import zio.prelude.Assertion.*
import zio.prelude.*

type TelegramMessageBody = TelegramMessageBody.Type
object TelegramMessageBody extends Subtype[String]:
  override inline def assertion =
    hasLength(greaterThan(0))

case class MessageDeliveryError(message: String, throwable: Throwable)

trait TelegramService:
  def sendMessage(messageBody: TelegramMessageBody): IO[MessageDeliveryError, Unit]

/** Service for interacting with Telegram bot API.
  *
  * API docs: https://core.telegram.org/bots/api
  */
case class LiveTelegramService(config: TelegramConfig, sttpBackend: SttpBackend[Task, Any]) extends TelegramService:
  import LiveTelegramService.*

  // https://core.telegram.org/bots/api#sendmessage
  override def sendMessage(messageBody: TelegramMessageBody): IO[MessageDeliveryError, Unit] =
    val queryParams = Map(
      "chat_id" -> config.chatId,
      "text"    -> messageBody
    )
    val url         = uri"https://api.telegram.org/bot${config.token}/sendMessage?$queryParams"
    val request     = basicRequest
      .get(url)
      .response(
        asEither(asJson[TelegramErrorResponse], ignore)
      ) // 2xx response indicates that the message delivery was successful

    val telegramApiResponse = sttpBackend
      .send(request)
      .map(_.body)

    val errorsHandled = telegramApiResponse
      .mapError(t => MessageDeliveryError("Network failure", t)) // Throwable indicates network failure
      .reject { case Left(errorResponse) => // Left indicates non 2xx response code
        errorResponse.fold(
          responseException => MessageDeliveryError("Response exception", responseException),
          telegramErr => MessageDeliveryError("Telegram API error", telegramErr)
        )
      }
      .unit

    ZIO.log(s"Sending message $messageBody") *> errorsHandled

case class TelegramConfig(token: String, chatId: String)

object LiveTelegramService:
  private case class TelegramErrorResponse(
      ok: Boolean,
      @jsonHint("error_code") errorCode: Int,
      description: String
  ) extends Exception(description)

  private given JsonCodec[TelegramErrorResponse] = DeriveJsonCodec.gen[TelegramErrorResponse]

  val layer = ZLayer.fromFunction(LiveTelegramService.apply)
