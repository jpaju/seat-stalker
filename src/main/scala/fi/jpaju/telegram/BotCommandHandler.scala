package fi.jpaju.telegram

import fi.jpaju.stalker.*
import zio.*

trait BotCommandHandler:
  def handle(command: BotCommand, context: MessageContext): IO[MessageDeliveryError, Unit]

class LiveBotCommandHandler(
    telegramClient: TelegramClient,
    stalkerJobRepository: StalkerJobRepository
) extends BotCommandHandler:

  def handle(command: BotCommand, context: MessageContext): IO[MessageDeliveryError, Unit] =
    command match
      case BotCommand.Echo(message) =>
        val messageBody = MessageFormatter.formatEcho(message)
        telegramClient.sendMessage(messageBody)

      case BotCommand.ListJobs =>
        for
          jobs       <- stalkerJobRepository.getAll
          messageBody = MessageFormatter.formatJobsList(jobs)
          _          <- telegramClient.sendMessage(messageBody)
        yield ()

object LiveBotCommandHandler:
  val layer = ZLayer.fromFunction(LiveBotCommandHandler.apply)
