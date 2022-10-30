package fi.jpaju.stalker

import fi.jpaju.*
import fi.jpaju.restaurant.*
import fi.jpaju.telegram.*
import zio.*

import java.time.LocalDate

case class StalkerJobDefinition(
    restaurant: Restaurant,
    persons: PersonCount
)

trait StalkerJobRunner:
  def runJob(jobDefinition: StalkerJobDefinition): UIO[Unit]

case class LiveStalkerJobRunner(
    tableService: TableService,
    telegramService: TelegramService
) extends StalkerJobRunner:
  def runJob(jobDefinition: StalkerJobDefinition): UIO[Unit] =
    for
      now         <- Clock.localDateTime.map(_.toLocalDate)
      _           <- ZIO.logDebug(s"Checking available tables in ${jobDefinition.restaurant.name} at $now")
      tableStatus <- checkTables(jobDefinition, now)
      _           <- ZIO.logDebug(s"Found tables in ${jobDefinition.restaurant.name}: $tableStatus")
      _           <- sendNotifications(jobDefinition.restaurant, tableStatus)
    yield ()

  private def checkTables(requirements: StalkerJobDefinition, when: LocalDate): UIO[TableStatus] =
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

  private def sendNotifications(restaurant: Restaurant, tableStatus: TableStatus): UIO[Unit] =
    tableStatus.availableTables
      .map { availableTables =>
        val message         = MessageFormatter.tablesAvailableMessage(restaurant, availableTables)
        val telegramMessage = ZIO
          .fromEither(TelegramMessageBody.make(message).toEither)
          .orDieWith(validationErrs =>
            new RuntimeException(s"Telegram message was nonempty: ${validationErrs.toString}")
          )

        val retryPolicy = Schedule.exponential(1.second) && Schedule.recurs(5)
        telegramMessage
          .flatMap(telegramService.sendMessage(_).retry(retryPolicy))
          .tapError(err => ZIO.logError(s"Failed to notify about available tables: $tableStatus"))
          .ignore
      }
      .getOrElse(ZIO.unit)

object LiveStalkerJobRunner:
  val layer = ZLayer.fromFunction(LiveStalkerJobRunner.apply)
