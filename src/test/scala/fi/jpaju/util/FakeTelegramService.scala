package fi.jpaju
package util

import zio.*

case class FakeTelegramService(ref: Ref[List[TelegramMessageBody]]) extends TelegramService:
  def sendMessage(message: TelegramMessageBody): UIO[Unit] =
    ref.update(_ :+ message)

object FakeTelegramService:
  val layer = ZLayer.fromFunction(FakeTelegramService.apply)
