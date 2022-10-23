package fi.jpaju.restaurant

import zio.*

import java.time.*

case class CheckTablesParameters(
    restaurant: Restaurant,
    persons: PersonCount,
    startingFrom: LocalDate
)

trait TableService:
  def checkAvailableTables(parameters: CheckTablesParameters): UIO[TableStatus]
