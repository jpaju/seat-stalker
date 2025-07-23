package fi.jpaju
package util

import fi.jpaju.telegram.*
import zio.*

type MessageFunction = TelegramMessageBody => IO[MessageDeliveryError, Unit]

/** Fake Telegram client that can be used for testing purposes. Collects sent messages and allows to customize how the
  * client reacts to messages being sent.
  *
  * @param msgFunction
  *   Reference to function that is used to compute the result of sending a message.
  */
case class FakeTelegramClient(
    msgFunction: Ref[MessageFunction],
    messages: Ref[Vector[TelegramMessageBody]]
) extends TelegramClient:
  def sendMessage(message: TelegramMessageBody): IO[MessageDeliveryError, Unit] =
    for
      _  <- messages.update(_ :+ message)
      fn <- msgFunction.get
      _  <- fn(message)
    yield ()

object FakeTelegramClient:
  def getSentMessages: URIO[FakeTelegramClient, Vector[TelegramMessageBody]] =
    ZIO.serviceWithZIO[FakeTelegramClient](_.messages.get)

  def resetSentMessages: URIO[FakeTelegramClient, Unit] =
    ZIO.serviceWithZIO[FakeTelegramClient](_.messages.set(Vector.empty))

  def setSendMessageFunction(fn: MessageFunction): URIO[FakeTelegramClient, Unit] =
    ZIO.serviceWithZIO[FakeTelegramClient](_.msgFunction.set(fn))

  val layer = ZLayer.fromZIO {
    val defaultFunction: MessageFunction = (_: TelegramMessageBody) => ZIO.unit

    for
      messages <- Ref.make(Vector.empty[TelegramMessageBody])
      msgFn    <- Ref.make(defaultFunction)
    yield FakeTelegramClient(msgFn, messages)
  }
