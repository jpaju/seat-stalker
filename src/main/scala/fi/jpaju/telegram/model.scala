package fi.jpaju.telegram

import zio.prelude.*
import zio.prelude.Assertion.*

type TelegramMessageBody = TelegramMessageBody.Type
object TelegramMessageBody extends Subtype[String]:
  override inline def assertion =
    hasLength(greaterThan(0))

case class MessageDeliveryError(message: String, throwable: Throwable)

type ChatId = ChatId.Type
object ChatId extends zio.prelude.Newtype[String]
