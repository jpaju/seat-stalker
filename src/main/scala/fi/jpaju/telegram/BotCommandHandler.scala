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
        val response    = s"Echo: $message" // TODO Factor message formatting to separate file/class
        val messageBody = TelegramMessageBody.wrap(response)
        telegramClient.sendMessage(messageBody)

      case BotCommand.ListJobs =>
        for
          jobs       <- stalkerJobRepository.getAll
          response    = formatJobsList(jobs) // TODO Factor message formatting to separate file/class
          messageBody = TelegramMessageBody.wrap(response)
          _          <- telegramClient.sendMessage(messageBody)
        yield ()

  private def formatJobsList(jobs: Set[StalkerJobDefinition]): String =
    if jobs.isEmpty then "No active jobs currently."
    else
      val jobsText = jobs.map(job => s"• ${job.restaurant.name} - ${job.persons} persons")
      s"Current monitoring jobs:\n${jobsText.mkString("\n")}"

object LiveBotCommandHandler:
  val layer = ZLayer.fromFunction(LiveBotCommandHandler.apply)
