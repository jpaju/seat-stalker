package fi.jpaju.telegram

import zio.config.magnolia.*

case class TelegramConfig(token: String, chatId: String)
object TelegramConfig:
  val config = deriveConfig[TelegramConfig].nested("TELEGRAM")
