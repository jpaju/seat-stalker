import sttp.client3.*
import sttp.client3.ziojson.*
import zio.*
import zio.json.*

trait TelegramService:
  def sendMessage(message: String): UIO[Unit]

case class LiveTelegramService(config: LiveTelegramService.Config, sttpBackend: SttpBackend[Task, Any])
    extends TelegramService:

  override def sendMessage(message: String): UIO[Unit] =
    val queryParams = Map(
      "chat_id" -> config.chatId,
      "text"    -> message
    )
    val url         = uri"https://api.telegram.org/bot${config.token}/sendMessage?$queryParams"
    val request     = basicRequest.get(url)
    sttpBackend.send(request).debug("Telegram").unit.orDie

object LiveTelegramService:
  case class Config(token: String, chatId: String)

  val layer = ZLayer.fromFunction(LiveTelegramService.apply)
