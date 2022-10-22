package fi.jpaju.telegram

import zio.prelude.Assertion.*
import zio.prelude.*

type TelegramMessageBody = TelegramMessageBody.Type
object TelegramMessageBody extends Subtype[String]:
  override inline def assertion =
    hasLength(greaterThan(0))

case class MessageDeliveryError(message: String, throwable: Throwable)

case class TelegramConfig(token: String, chatId: String)
