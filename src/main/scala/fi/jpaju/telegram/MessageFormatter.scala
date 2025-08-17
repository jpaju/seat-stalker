package fi.jpaju.telegram

import fi.jpaju.restaurant.*
import fi.jpaju.stalker.*

import java.time.format.DateTimeFormatter

object MessageFormatter:
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm d.M.yyyy");

  def tablesAvailableMessage(restaurant: Restaurant, availableTables: List[AvailableTable]): TelegramMessageBody =
    val tables = availableTables
      .map { table =>
        val formattedTime = table.time.format(dateTimeFormatter)
        s"Table for ${table.persons} on $formattedTime"
      }
      .mkString("\n - ")

    TelegramMessageBody.wrap(s"Free tables available in ${restaurant.name} 🍔:\n - $tables")

  def formatEcho(message: String): TelegramMessageBody =
    TelegramMessageBody.wrap(s"Echo: $message")

  def formatJobsList(jobs: Set[StalkerJobDefinition]): TelegramMessageBody =
    val text =
      if jobs.isEmpty then "No active jobs currently."
      else
        val jobsText = jobs.map(job => s"• ${job.restaurant.name} - ${job.persons} persons")
        s"Current monitoring jobs:\n${jobsText.mkString("\n")}"
    TelegramMessageBody.wrap(text)
