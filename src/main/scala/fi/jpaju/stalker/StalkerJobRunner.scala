package fi.jpaju.stalker

import fi.jpaju.Logging
import fi.jpaju.*
import fi.jpaju.restaurant.*
import fi.jpaju.telegram.*
import zio.*

import java.time.LocalDateTime

case class StalkerJobDefinition(
    restaurant: Restaurant,
    persons: PersonCount
)

trait StalkerJobRunner:
  def runJob(jobDefinition: StalkerJobDefinition): UIO[Unit]

case class LiveStalkerJobRunner(
    tableService: TableService,
    telegramClient: TelegramClient
) extends StalkerJobRunner:
  def runJob(jobDefinition: StalkerJobDefinition): UIO[Unit] =
    val spanName = s"stalker-job-${jobDefinition.restaurant.name}-${jobDefinition.persons}"
    ZIO.logSpan(Logging.formatLogSpanName(spanName)) {
      for
        today       <- Clock.localDateTime
        _           <- ZIO.log(s"Checking available tables in ${jobDefinition.restaurant.name} from $today")
        tableStatus <- checkTables(jobDefinition, today)
        _           <- tableStatus.fold(
                         whenAvailable = (restaurant, tables) =>
                           ZIO.log(s"Found tables in ${restaurant.name}: $tables") *> sendNotifications(restaurant, tables),
                         whenNotAvailable = restaurant => ZIO.log(s"No available tables were found in ${restaurant.name}")
                       )
      yield ()
    }

  private def checkTables(requirements: StalkerJobDefinition, when: LocalDateTime): UIO[TableStatus] =
    val checkTablesParameters = CheckTablesParameters(
      requirements.restaurant,
      requirements.persons,
      when
    )

    val maxTables = 10
    tableService
      .checkAvailableTables(checkTablesParameters)
      .take(maxTables)
      .runCollect
      .map { tables =>
        if tables.isEmpty then TableStatus.NotAvailable(requirements.restaurant)
        else TableStatus.Available(requirements.restaurant, tables.toList)
      }
  end checkTables

  private def sendNotifications(restaurant: Restaurant, availableTables: List[AvailableTable]): UIO[Unit] =
    val message         = MessageFormatter.tablesAvailableMessage(restaurant, availableTables)
    val telegramMessage = ZIO
      .fromEither(TelegramMessageBody.make(message).toEither)
      .orDieWith(validationErrs => new RuntimeException(s"Telegram message was nonempty: ${validationErrs.toString}"))

    telegramMessage
      .flatMap(telegramClient.sendMessage(_))
      .tapError(err =>
        ZIO.logError(s"Encountered an error: $err, when trying to notify about tables: $availableTables")
      )
      .orDieWith(err => new RuntimeException(s"Failed to notify about available tables: $err"))

object LiveStalkerJobRunner:
  val layer = ZLayer.fromFunction(LiveStalkerJobRunner.apply)
