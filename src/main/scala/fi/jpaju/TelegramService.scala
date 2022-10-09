package fi.jpaju

import sttp.client3.*
import sttp.client3.ziojson.*
import zio.*
import zio.json.*

trait TelegramService:
  def sendMessage(message: String): UIO[Unit]

case class LiveTelegramService(config: TelegramConfig, sttpBackend: SttpBackend[Task, Any]) extends TelegramService:

  override def sendMessage(message: String): UIO[Unit] =
    val queryParams = Map(
      "chat_id" -> config.chatId,
      "text"    -> message
    )
    val url         = uri"https://api.telegram.org/bot${config.token}/sendMessage?$queryParams"
    val request     = basicRequest.get(url)
    sttpBackend.send(request).debug("Telegram").unit.orDie

case class TelegramConfig(token: String, chatId: String)
object LiveTelegramService:
  val layer = ZLayer.fromFunction(LiveTelegramService.apply)
