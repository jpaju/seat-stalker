package fi.jpaju.telegram

import fi.jpaju.restaurant.*
import fi.jpaju.stalker.*
import zio.*

import java.time.format.DateTimeFormatter

trait MessageFormatter:
  def tablesAvailableMessage(restaurant: Restaurant, availableTables: List[AvailableTable]): TelegramMessageBody
  def formatEcho(message: String): TelegramMessageBody
  def formatJobsList(jobs: Set[StalkerJobDefinition]): TelegramMessageBody

class LiveMessageFormatter extends MessageFormatter:
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm d.M.yyyy");

  def tablesAvailableMessage(restaurant: Restaurant, availableTables: List[AvailableTable]): TelegramMessageBody =
    val tables = availableTables
      .map { table =>
        val formattedTime = table.time.format(dateTimeFormatter)
        s"🪑 Table for ${table.persons} ${personText(table.persons)} at $formattedTime"
      }
      .mkString("\n")

    TelegramMessageBody.wrap(s"🎉 Great news! Free tables available at ${restaurant.name} 🍽️\n\n$tables")

  def formatEcho(message: String): TelegramMessageBody =
    TelegramMessageBody.wrap(s"🔊 Echo: $message")

  def formatJobsList(jobs: Set[StalkerJobDefinition]): TelegramMessageBody =
    val text =
      if jobs.isEmpty then "📭 No active monitoring jobs currently."
      else
        val jobsText = jobs.map { job =>
          s"🔍 ${job.restaurant.name} - ${job.persons} ${personText(job.persons)}"
        }
        s"📋 Current monitoring jobs:\n\n${jobsText.mkString("\n")}"

    TelegramMessageBody.wrap(text)

  private def personText(count: PersonCount): String =
    if count == PersonCount(1) then "person" else "people"

object LiveMessageFormatter:
  val layer = ZLayer.succeed(LiveMessageFormatter())
