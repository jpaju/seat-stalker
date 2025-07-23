package fi.jpaju.telegram

import io.circe.parser.decode
import telegramium.bots
import telegramium.bots.CirceImplicits.given
import zio.*
import java.time.Instant

trait BotController:
  def handleWebhook(jsonPayload: String): IO[BotError, Unit]

class LiveBotController(botCommandHandler: BotCommandHandler) extends BotController:
  override def handleWebhook(jsonPayload: String): IO[BotError, Unit] =
    // TODO Verify secret token from request
    val run = for
      update  <- decodePayload(jsonPayload)
      message <- parseMessage(update)
      command <- parseCommand(message)
      ctx      = getMessageContext(message)
      _       <- botCommandHandler
                   .handle(command, ctx)
                   .mapError(BotError.DeliveryError(_))
    yield ()

    // TODO Report errors either by sending message to user or logging
    run.catchAll {
      case BotError.ParseError(message)   => ZIO.unit
      case BotError.CommandError(message) => ZIO.unit
      case BotError.DeliveryError(error)  => ZIO.unit
    }

  // TODO Refactor decoding to another class/file?
  private def decodePayload(jsonPayload: String) =
    ZIO
      .fromEither(decode[bots.Update](jsonPayload))
      .orElseFail(BotError.ParseError("Cannot decode request JSON"))

  private def parseMessage(update: bots.Update) =
    ZIO
      .fromOption(update.message)
      .orElseFail(BotError.ParseError("Update did not contain message"))

  private def parseCommand(message: bots.Message): IO[BotError, BotCommand] =
    ZIO.fromEither {
      for
        text    <- message.text.toRight(BotError.CommandError("Message text was empty"))
        command <- BotCommand.fromText(text).toRight(BotError.CommandError(s"Unknown command $text"))
      yield command
    }

  private def getMessageContext(message: bots.Message): MessageContext =
    val chatId    = getChatId(message)
    val timestamp = getMessageTimestamp(message)
    MessageContext(chatId, timestamp)

  private def getChatId(message: bots.Message): ChatId =
    ChatId(message.chat.id.toString)

  private def getMessageTimestamp(message: bots.Message): Instant =
    Instant.ofEpochSecond(message.date)

object LiveBotController:
  val layer = ZLayer.fromFunction(LiveBotController.apply)
