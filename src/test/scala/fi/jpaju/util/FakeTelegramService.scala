package fi.jpaju
package util

import fi.jpaju.telegram.*
import zio.*

type MessageFunction = TelegramMessageBody => IO[MessageDeliveryError, Unit]

/** Fake Telegram service that can be used for testing purposes. Collects sent messages and allows to customize how the
  * service reactts to messages being sent.
  *
  * @param msgFunction
  *   Reference to function that is used to compute the result of sending a message.
  */
case class FakeTelegramService(
    msgFunction: Ref[MessageFunction],
    messages: Ref[Vector[TelegramMessageBody]]
) extends TelegramService:
  def sendMessage(message: TelegramMessageBody): IO[MessageDeliveryError, Unit] =
    for
      _  <- messages.update(_ :+ message)
      fn <- msgFunction.get
      _  <- fn(message)
    yield ()

object FakeTelegramService:
  def getSentMessages: URIO[FakeTelegramService, Vector[TelegramMessageBody]] =
    ZIO.serviceWithZIO[FakeTelegramService](_.messages.get)

  def resetSentMessages: URIO[FakeTelegramService, Unit] =
    ZIO.serviceWithZIO[FakeTelegramService](_.messages.set(Vector.empty))

  def setSendMessageFunction(fn: MessageFunction): URIO[FakeTelegramService, Unit] =
    ZIO.serviceWithZIO[FakeTelegramService](_.msgFunction.set(fn))

  val layer = ZLayer.fromZIO {
    val defaultFunction: MessageFunction = msg => ZIO.unit

    for
      messages <- Ref.make(Vector.empty[TelegramMessageBody])
      msgFn    <- Ref.make(defaultFunction)
    yield FakeTelegramService(msgFn, messages)
  }
