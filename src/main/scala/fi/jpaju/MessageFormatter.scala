package fi.jpaju

import fi.jpaju.restaurant.*
import fi.jpaju.telegram.*

import java.time.format.DateTimeFormatter

object MessageFormatter:
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm d.M.yyyy");

  def tablesAvailableMessage(restaurant: Restaurant, availableTables: List[AvailableTable]): String =
    val tables = availableTables
      .map(formatAvailableTable)
      .mkString("\n - ")

    s"Free tables available in ${restaurant.name} 🍔:\n - $tables"

  private def formatAvailableTable(table: AvailableTable): String =
    val formattedTime = table.time.format(dateTimeFormatter)
    s"Table for ${table.persons} on $formattedTime"
