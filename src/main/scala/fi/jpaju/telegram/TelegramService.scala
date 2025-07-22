package fi.jpaju.telegram

import sttp.client3.*
import sttp.client3.ziojson.*
import zio.*
import zio.config.magnolia.*
import zio.json.*

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
      .response(asEither(asJson[TelegramErrorResponse], ignore)) // 2xx response indicates successful delivery

    val telegramApiResponse = sttpBackend
      .send(request)
      .mapError(t => MessageDeliveryError("Network failure", t)) // Throwable indicates network failure
      .map(_.body)
      .reject {
        case Left(errorResponse) => // Left indicates non 2xx response code
          errorResponse.fold(
            responseException => MessageDeliveryError("Response exception", responseException),
            telegramErr => MessageDeliveryError("Telegram API error", telegramErr)
          )
      }

    telegramApiResponse
      .tapBoth(
        deliveryError => ZIO.logWarning(s"Failed sending telegram message: $messageBody, with error: $deliveryError"),
        _ => ZIO.log(s"Message delivered to Telegram: $messageBody")
      )
      .unit

object LiveTelegramService:
  private case class TelegramErrorResponse(
      ok: Boolean,
      @jsonHint("error_code") errorCode: Int,
      description: String
  ) extends Exception(description)

  private given JsonCodec[TelegramErrorResponse] = DeriveJsonCodec.gen[TelegramErrorResponse]

  val layer = ZLayer:
    for
      config      <- ZIO.config(TelegramConfig.config.nested("TELEGRAM"))
      sttpBackend <- ZIO.service[SttpBackend[Task, Any]]
    yield LiveTelegramService(config, sttpBackend)

case class TelegramConfig(token: String, chatId: String)
object TelegramConfig:
  val config = deriveConfig[TelegramConfig]
