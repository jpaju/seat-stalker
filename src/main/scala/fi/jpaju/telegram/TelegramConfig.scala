package fi.jpaju.telegram

import zio.config.magnolia.*

case class TelegramConfig(botToken: String, chatId: String, secretToken: String)
object TelegramConfig:
  val config = deriveConfig[TelegramConfig].nested("TELEGRAM")
