package fi.jpaju

import sttp.client3.*
import sttp.client3.ziojson.*
import zio.*
import zio.json.*
import zio.prelude.*
import zio.prelude.Assertion.*

type TelegramMessageBody = TelegramMessageBody.Type
object TelegramMessageBody extends Subtype[String]:
  override inline def assertion =
    hasLength(greaterThan(0))

trait TelegramService:
  def sendMessage(messageBody: TelegramMessageBody): UIO[Unit]

/** Service for interacting with Telegram bot API.
  *
  * API docs: https://core.telegram.org/bots/api
  */
case class LiveTelegramService(config: TelegramConfig, sttpBackend: SttpBackend[Task, Any]) extends TelegramService:

  // https://core.telegram.org/bots/api#sendmessage
  override def sendMessage(messageBody: TelegramMessageBody): UIO[Unit] =
    if messageBody.isEmpty then ZIO.unit
    else
      val queryParams = Map(
        "chat_id" -> config.chatId,
        "text"    -> messageBody
      )
      val url         = uri"https://api.telegram.org/bot${config.token}/sendMessage?$queryParams"
      val request     = basicRequest.get(url)
      sttpBackend.send(request).unit.orDie

case class TelegramConfig(token: String, chatId: Int)
object LiveTelegramService:
  val layer = ZLayer.fromFunction(LiveTelegramService.apply)
